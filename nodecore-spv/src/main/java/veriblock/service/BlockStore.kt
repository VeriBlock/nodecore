package veriblock.service

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import java.io.File
import java.io.RandomAccessFile
import java.math.BigInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BlockStore(
    networkParameters: NetworkParameters,
    baseDir: File
) {
    private val lock = ReentrantLock()

    private val indexFile = baseDir.toPath().resolve("$networkParameters-block-index.db").toFile()
    private val blocksFile = baseDir.toPath().resolve("$networkParameters-blocks.db").toFile()

    class BlockIndex(
        var tip: StoredVeriBlockBlock,
        val fileIndex: MutableMap<VBlakeHash, Long>
    ) {
        val truncatedFileIndex: MutableMap<VBlakeHash, Long> = fileIndex.entries.associate { (hash, index) ->
            hash.trimToPreviousBlockSize() to index
        }.toMutableMap()
    }

    val index = initializeIndex(networkParameters)

    private fun initializeIndex(networkParameters: NetworkParameters): BlockIndex {
        return if (!indexFile.exists()) {
            val genesisBlock = StoredVeriBlockBlock(
                networkParameters.genesisBlock,
                BitcoinUtilities.decodeCompactBits(networkParameters.genesisBlock.difficulty.toLong()),
                networkParameters.genesisBlock.hash
            )
            RandomAccessFile(blocksFile, "rw").use { file ->
                file.seek(0)
                file.writeInt(genesisBlock.height)
                file.write(genesisBlock.hash.bytes)
                file.write(Utility.toBytes(genesisBlock.work, StoredVeriBlockBlock.CHAIN_WORK_BYTES))
                file.write(genesisBlock.block.raw)
            }
            RandomAccessFile(indexFile, "rw").use {
                it.writeInt(1)
                it.write(genesisBlock.hash.bytes)
                it.write(genesisBlock.hash.bytes)
                it.writeLong(0L)
            }
            BlockIndex(genesisBlock, mutableMapOf(VBlakeHash.EMPTY_HASH to 0L))
        } else {
            readIndex()
        }
    }

    fun readIndex(): BlockIndex = RandomAccessFile(indexFile, "r").use { file ->
        val count = file.readInt()
        val tipHash = ByteArray(VBlakeHash.VERIBLOCK_LENGTH)
        file.read(tipHash)
        val tip = VBlakeHash.wrap(tipHash)
        val fileIndex = (0 until count).associate {
            val hash = ByteArray(VBlakeHash.VERIBLOCK_LENGTH)
            file.read(hash)
            VBlakeHash.wrap(hash) to file.readLong()
        }.toMutableMap()
        BlockIndex(readBlock(fileIndex.getValue(tip)), fileIndex)
    }

    fun getTip(): StoredVeriBlockBlock = index.tip

    fun setTip(tip: StoredVeriBlockBlock) = lock.withLock {
        index.tip = tip
        RandomAccessFile(indexFile, "rw").use { file ->
            file.seek(4)
            file.write(tip.hash.bytes)
        }
    }

    fun getFileIndex(hash: VBlakeHash) = index.fileIndex[hash] ?: index.truncatedFileIndex[hash]

    fun readBlock(hash: VBlakeHash): StoredVeriBlockBlock? {
        val filePosition = getFileIndex(hash)
            ?: return null

        return readBlock(filePosition)
    }

    fun readBlock(filePosition: Long): StoredVeriBlockBlock {
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
            file.writeInt(block.height)
            file.write(block.hash.bytes)
            file.write(Utility.toBytes(block.work, StoredVeriBlockBlock.CHAIN_WORK_BYTES))
            file.write(block.block.raw)
            filePosition
        }
        index.fileIndex[block.hash] = blockFilePosition
        index.truncatedFileIndex[block.hash.trimToPreviousBlockSize()] = blockFilePosition
        RandomAccessFile(indexFile, "rw").use { file ->
            val blockCount = file.readInt()
            file.seek(0)
            file.writeInt(blockCount + 1)
            file.seek(file.length())
            file.write(block.hash.bytes)
            file.writeLong(blockFilePosition)
        }
    }

    fun writeBlocks(blocks: List<StoredVeriBlockBlock>) = lock.withLock {
        val blockFilePosition = RandomAccessFile(blocksFile, "rw").use { file ->
            val filePosition = file.length()
            file.seek(filePosition)
            blocks.forEach { block ->
                file.writeInt(block.height)
                file.write(block.hash.bytes)
                file.write(Utility.toBytes(block.work, StoredVeriBlockBlock.CHAIN_WORK_BYTES))
                file.write(block.block.raw)
            }
            filePosition
        }

        RandomAccessFile(indexFile, "rw").use { file ->
            val blockCount = file.readInt()
            file.seek(0)
            file.writeInt(blockCount + blocks.size)
            file.seek(file.length())
            var cumulativeFilePosition = blockFilePosition
            for (block in blocks) {
                index.fileIndex[block.hash] = cumulativeFilePosition
                index.truncatedFileIndex[block.hash.trimToPreviousBlockSize()] = cumulativeFilePosition
                file.write(block.hash.bytes)
                file.writeLong(cumulativeFilePosition)
                cumulativeFilePosition += 4 + VBlakeHash.VERIBLOCK_LENGTH + StoredVeriBlockBlock.CHAIN_WORK_BYTES + block.block.raw.size
            }
        }
    }
}
