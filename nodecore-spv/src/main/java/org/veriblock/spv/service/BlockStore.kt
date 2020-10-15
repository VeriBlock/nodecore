package org.veriblock.spv.service

import io.ktor.utils.io.*
import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.debugWarn
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.math.BigInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = createLogger {}

class BlockStore(
    private val networkParameters: NetworkParameters,
    baseDir: File
) {
    private val lock = ReentrantLock()
    private val blocksCache = LRUCache<VBlakeHash, StoredVeriBlockBlock>(VBK_MAX_REORG_DISTANCE + 1)

    private val indexFile = File(baseDir, "$networkParameters-block-index.db")
    private val blocksFile = File(baseDir,"$networkParameters-blocks.db")

    private fun writeBlockBody(file: RandomAccessFile, block: StoredVeriBlockBlock) {
        file.writeInt(block.height)
        file.write(block.hash.bytes)
        file.write(Utility.toBytes(block.work, StoredVeriBlockBlock.CHAIN_WORK_BYTES))
        file.write(block.header.raw)
    }

    class BlockIndex(
        var tip: StoredVeriBlockBlock,
        val fileIndex: MutableMap<VBlakeHash, Long>
    )

    val index = initializeIndex()

    private fun initializeIndex(): BlockIndex {
        return if (!indexFile.exists()) {
            createGenesisIndex()
        } else {
            readIndex()
        }
    }

    private fun createGenesisIndex(): BlockIndex {
        indexFile.delete()
        blocksFile.delete()
        val genesisBlock = StoredVeriBlockBlock(
            networkParameters.genesisBlock,
            BitcoinUtilities.decodeCompactBits(networkParameters.genesisBlock.difficulty.toLong()),
            networkParameters.genesisBlock.hash
        )
        RandomAccessFile(blocksFile, "rw").use { file ->
            file.seek(0)
            writeBlockBody(file, genesisBlock)
        }
        RandomAccessFile(indexFile, "rw").use {
            it.writeInt(1)
            it.write(genesisBlock.hash.bytes)
            it.write(genesisBlock.hash.bytes)
            it.writeLong(0L)
        }
        return BlockIndex(genesisBlock, mutableMapOf(genesisBlock.hash.trimToPreviousBlockSize() to 0L))
    }

    private fun readChainWithTip(hash: VBlakeHash, size: Int): ArrayList<StoredVeriBlockBlock> {
        val ret = ArrayList<StoredVeriBlockBlock>()
        var i = 0
        var cursor = readBlock(hash)
        while (cursor != null && i++ < size) {
            ret.add(cursor)
            cursor = readBlock(cursor.header.previousBlock)
        }
        ret.reverse()
        return ret
    }

    fun getTip(): StoredVeriBlockBlock = index.tip

    fun setTip(tip: StoredVeriBlockBlock) = lock.withLock {
        index.tip = tip
        RandomAccessFile(indexFile, "rw").use { file ->
            file.seek(4)
            file.write(tip.hash.bytes)
        }
    }

    private fun getFileIndex(hash: VBlakeHash) = index.fileIndex[hash.trimToPreviousBlockSize()]

    fun readBlock(hash: VBlakeHash): StoredVeriBlockBlock? {
        val shortHash = hash.trimToPreviousBlockSize()
        val entry = blocksCache[shortHash]
        if (entry != null) {
            return entry
        }

        val filePosition = getFileIndex(shortHash)
            ?: return null

        val block = readBlock(filePosition)
        blocksCache[shortHash] = block
        return block
    }

    fun exists(hash: VBlakeHash): Boolean {
        return getFileIndex(hash) != null
    }

    private fun readBlock(filePosition: Long): StoredVeriBlockBlock {
        return RandomAccessFile(blocksFile, "r").use { file ->
            file.seek(filePosition)
            val blockHeight = file.readInt()
            val blockHash = ByteArray(VBlakeHash.VERIBLOCK_LENGTH)
            file.read(blockHash)
            val workBytes = ByteArray(StoredVeriBlockBlock.CHAIN_WORK_BYTES)
            file.read(workBytes)
            val work = BigInteger(1, workBytes)
            val blockHeader = ByteArray(BlockUtility.getBlockHeaderLength(blockHeight))
            file.read(blockHeader)
            val fullHash = VBlakeHash.wrap(blockHash)
            StoredVeriBlockBlock(
                SerializeDeserializeService.parseVeriBlockBlock(blockHeader, fullHash),
                work,
                fullHash
            )
        }
    }

    fun writeBlock(block: StoredVeriBlockBlock) = lock.withLock {
        val blockFilePosition = RandomAccessFile(blocksFile, "rw").use { file ->
            val filePosition = file.length()
            file.seek(filePosition)
            writeBlockBody(file, block)
            filePosition
        }
        val smallHash = block.hash.trimToPreviousBlockSize()

        index.fileIndex[smallHash] = blockFilePosition
        RandomAccessFile(indexFile, "rw").use { file ->
            val blockCount = file.readInt()
            file.seek(0)
            file.writeInt(blockCount + 1)
            file.seek(file.length())
            file.write(block.hash.bytes)
            file.writeLong(blockFilePosition)
        }

        // block that is "just written" is very likely to be accessed very soon
        blocksCache[smallHash] = block
    }

    fun writeBlocks(blocks: List<StoredVeriBlockBlock>) = lock.withLock {
        val blockFilePosition = RandomAccessFile(blocksFile, "rw").use { file ->
            val filePosition = file.length()
            file.seek(filePosition)
            blocks.forEach { block ->
                writeBlockBody(file, block)
            }
            filePosition
        }

        RandomAccessFile(indexFile, "rw").use { file ->
            val blockCount = file.readInt()
            file.seek(0)
            file.writeInt(blockCount + blocks.size)
            file.seek(4 + VBlakeHash.VERIBLOCK_LENGTH + blockCount.toLong() * (VBlakeHash.VERIBLOCK_LENGTH + 8))
            var cumulativeFilePosition = blockFilePosition
            for (block in blocks) {
                val smallHash = block.hash.trimToPreviousBlockSize()
                index.fileIndex[smallHash] = cumulativeFilePosition
                file.write(block.hash.bytes)
                file.writeLong(cumulativeFilePosition)
                cumulativeFilePosition += 4 + VBlakeHash.VERIBLOCK_LENGTH + StoredVeriBlockBlock.CHAIN_WORK_BYTES + block.header.raw.size
            }
        }
    }

    private fun readIndex(): BlockIndex = RandomAccessFile(indexFile, "r").use { file ->
        val count = file.readInt()
        val tipHash = ByteArray(VBlakeHash.VERIBLOCK_LENGTH)
        file.read(tipHash)
        val tip = VBlakeHash.wrap(tipHash)

        val fileIndex = try {
            (0 until count).associate {
                val hash = ByteArray(VBlakeHash.VERIBLOCK_LENGTH)
                file.read(hash)
                VBlakeHash.wrap(hash) to file.readLong()
            }.toMutableMap()
        } catch (e: IOException) {
            logger.debugWarn(e) { "Error while loading blockchain index file. Redownloading blockchain from scratch..." }
            return createGenesisIndex()
        }
        val tipSmallHash = tip.trimToPreviousBlockSize()
        val tipFileIndex = fileIndex[tipSmallHash] ?: run {
            logger.warn { "Invalid blockchain tip! Redownloading blockchain from scratch..." }
            return createGenesisIndex()
        }
        BlockIndex(readBlock(tipFileIndex), fileIndex)
    }
}
