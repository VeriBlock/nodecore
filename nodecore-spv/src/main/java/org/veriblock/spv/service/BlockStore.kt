package org.veriblock.spv.service

import org.veriblock.core.bitcoinj.BitcoinUtilities
import org.veriblock.core.crypto.AnyVbkHash
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.VBK_HASH_LENGTH
import org.veriblock.core.crypto.VbkHash
import org.veriblock.core.params.NetworkParameters
import org.veriblock.core.utilities.*
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock
import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock.Companion.CHAIN_WORK_BYTES
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.IllegalStateException
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = createLogger {}

class BlockStore(
    private val networkParameters: NetworkParameters,
    baseDir: File
) {
    private val lock = ReentrantLock()
    private val blocksFile = RandomAccessFile(File(baseDir, "$networkParameters-blocks.db"), "rw")

    // in-memory block index
    val blockIndex = ConcurrentHashMap<PreviousBlockVbkHash, BlockIndex>()
    lateinit var activeChain: Chain

    init {
        // reads blocks file and builds block index
        reindex()
    }

    fun reindex() = lock.withLock {
        logger.info { "Reading $networkParameters blocks..." }

        // drop previous block index
        blockIndex.clear()

        // write genesis block to block index and block storage
        writeGenesisBlock(networkParameters.genesisBlock)

        val size = blocksFile.length()
        blocksFile.seek(0)

        var position = 0L

        // all blocks in block store must connect to their previousBlocks.
        // if block does not connect to prev block, we truncate blocksFile to
        // last valid block.
        while (blocksFile.filePointer < size) {
            try {
                position = blocksFile.filePointer
                // this moves filePointer forward
                val block = readBlockFromFile()
                if (block.height == 0) {
                    // skip genesis block
                    continue
                }
                // we were able to read a block from blocks file.
                // this block must connect to previous block, i.e.
                // previous block must exist in index
                val prevHash = block.header.previousBlock
                val prevIndex = blockIndex[prevHash]
                if (prevIndex == null) {
                    logger.warn { "Found block that does not connect to blockchain: height=${block.height} hash=${block.hash} " }
                    // this block does not connect to previous, move file
                    // pointer to position "before" this block and truncate file.
                    // this removes all next blocks, as we can not rely on their validity anymore.
                    blocksFile.channel.truncate(position)
                    // we no longer interested in reading
                    break
                }

                // prev index exists! this is valid block
                val smallHash = block.hash.trimToPreviousBlockSize()
                val index = BlockIndex(
                    hash = smallHash,
                    position = position,
                    height = block.height,
                    prev = prevIndex
                )
                blockIndex[smallHash] = index

                // update active chain
                if (activeChain.tipWork < block.work) {
                    activeChain.setTip(index, block.work)
                }
            } catch (e: IOException) {
                // we tried to read a block, but we were not able to do so.
                // move ptr to last valid block position and truncate the rest
                blocksFile.channel.truncate(position)
                break
            }
        }

        logger.info { "Successfully initialized Blockchain with ${blockIndex.size} blocks" }
    }

    fun getBlockIndex(hash: AnyVbkHash): BlockIndex? {
        return blockIndex[hash.trimToPreviousBlockSize()]
    }

    fun getChainHead(): StoredVeriBlockBlock? {
        return readBlock(activeChain.tip.position)
    }

    /**
     * @invariant should be called on valid unique connecting blocks
     */
    fun appendBlock(block: StoredVeriBlockBlock) = lock.withLock {
        val smallHash = block.hash.trimToPreviousBlockSize()
        if (blockIndex.containsKey(smallHash)) {
            // block already exists
            return
        }

        val position = blocksFile.length()
        blocksFile.seek(position)
        writeBlockToFile(block)
        appendToBlockIndex(position, block)
    }

    fun readBlock(hash: AnyVbkHash): StoredVeriBlockBlock? {
        val smallHash = hash.trimToPreviousBlockSize()
        val index = blockIndex[smallHash] ?: return null
        return readBlock(index.position)
    }

    fun readBlock(position: Long): StoredVeriBlockBlock? = lock.withLock {
        blocksFile.seek(position)
        return try {
            readBlockFromFile()
        } catch (e: IOException) {
            logger.error { "Can not read block at position ${position}: $e" }
            null
        }
    }

    fun readChainWithTip(hash: AnyVbkHash, size: Int): ArrayList<StoredVeriBlockBlock> {
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

    private fun appendToBlockIndex(position: Long, block: StoredVeriBlockBlock) {
        val smallHash = block.hash.trimToPreviousBlockSize()
        val prev = blockIndex[block.header.previousBlock]
        // this invariant should never fail with correct usage of this func
            ?: throw IllegalStateException(
                "Invariant failed! Can append to block index blocks that connect to block index."
            )

        val index = BlockIndex(
            hash = smallHash,
            position = position,
            height = block.height,
            prev = prev
        )
        blockIndex[smallHash] = index
    }

    // should seek to correct position before use
    private fun writeBlockToFile(block: StoredVeriBlockBlock) {
        writeBlockToFile(block.height, block.hash, block.work, block.header)
    }

    // should seek to correct position before use
    private fun writeBlockToFile(height: Int, hash: VbkHash, work: BigInteger, header: VeriBlockBlock) {
        check(lock.isLocked)

        blocksFile.writeInt(height)
        blocksFile.write(hash.bytes)
        blocksFile.write(Utility.toBytes(work, CHAIN_WORK_BYTES))
        blocksFile.write(header.raw)
    }

    // should seek to correct position before use
    private fun readBlockFromFile(): StoredVeriBlockBlock {
        check(lock.isLocked)

        val height = blocksFile.readInt()
        val hash = ByteArray(VBK_HASH_LENGTH)
        blocksFile.read(hash)
        val work = ByteArray(CHAIN_WORK_BYTES)
        blocksFile.read(work)
        val header = ByteArray(BlockUtility.getBlockHeaderLength(height))
        blocksFile.read(header)

        val vbkhash = VbkHash(hash)
        return StoredVeriBlockBlock(
            header = SerializeDeserializeService.parseVeriBlockBlock(header, vbkhash),
            work = BigInteger(1, work),
            hash = vbkhash
        )
    }

    private fun writeGenesisBlock(genesis: VeriBlockBlock) {
        val smallHash = genesis.hash.trimToPreviousBlockSize()
        val work = BitcoinUtilities.decodeCompactBits(networkParameters.genesisBlock.difficulty.toLong())

        blocksFile.seek(0)
        // write gb to position 0
        writeBlockToFile(
            block = StoredVeriBlockBlock(
                header = networkParameters.genesisBlock,
                work = work,
                hash = networkParameters.genesisBlock.hash
            )
        )

        // block index always contains genesis block on start
        val index = BlockIndex(
            hash = smallHash,
            position = 0, // gb is at position 0
            height = 0,
            prev = null
        )

        blockIndex[smallHash] = index
        activeChain = Chain(index, work)
    }
}
