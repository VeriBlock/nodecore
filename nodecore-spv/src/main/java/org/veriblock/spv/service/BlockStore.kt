package org.veriblock.spv.service

import kotlinx.coroutines.yield
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
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.IllegalStateException
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = createLogger {}

/**
 * @invariant blocks.db file should contain only connected blocks with correct work. I.e. store never
 * contains a block whose previousBlock does not exist in this store **before** this block.
 */
class BlockStore(
    val networkParameters: NetworkParameters,
    baseDir: File
) : Closeable, AutoCloseable {

    val path = File(baseDir, "$networkParameters-blocks.db")
    private val lock = ReentrantLock()
    private val blocksFile = RandomAccessFile(path, "rw")

    val size: Long get() = blocksFile.length()

    /**
     * Reads all blocks one-by-one from blocks file.
     * Executes `onBlock` on every new block found.
     *
     * If `onBlock` returns false, current block is invalid - truncates blocks file and returns.
     */
    fun forEach(
        onBlock: (position: Long, block: StoredVeriBlockBlock) -> Boolean
    ) = lock.withLock {
        val size = blocksFile.length()
        blocksFile.seek(0)
        var position = 0L

        // blocks are written in sequentially.
        // read all indices
        while (blocksFile.filePointer < size) {
            try {
                position = blocksFile.filePointer
                // this moves filePointer forward
                val block = readBlockFromFile()
                if (!onBlock(position, block)) {
                    // this block is invalid, move file
                    // pointer to position "before" this block and truncate the file.
                    // this removes all next blocks, as we can not rely on their validity anymore.
                    truncate(position)
                    // we no longer interested in reading
                    break
                }
            } catch (e: IOException) {
                // we tried to read a block, but we were not able to do so.
                // move ptr to last valid block position and truncate the rest
                truncate(position)
                break
            }
        }  // end while
    }

    /**
     * Writes a block to a given position.
     * @return end of written block
     */
    fun writeBlock(position: Long, block: StoredVeriBlockBlock): Long = lock.withLock {
        blocksFile.seek(position)
        writeBlockToFile(block)
        return blocksFile.filePointer
    }

    /**
     * Appends new block at the end of block storage.
     * @return its position in a file
     */
    fun appendBlock(block: StoredVeriBlockBlock): Long = lock.withLock {
        val position = blocksFile.length()
        writeBlock(position, block)
        return position
    }

    fun truncate(position: Long) {
        blocksFile.channel.truncate(position)
    }

    /**
     * Reads block at given position in blocks file.
     */
    fun readBlock(position: Long): StoredVeriBlockBlock? = lock.withLock {
        blocksFile.seek(position)
        return try {
            readBlockFromFile()
        } catch (e: IOException) {
            logger.error { "Can not read block at position ${position}: $e" }
            null
        }
    }

    /**
     * @invariant should seek to correct position before use
     */
    private fun writeBlockToFile(block: StoredVeriBlockBlock) {
        writeBlockToFile(block.height, block.hash, block.work, block.header)
    }

    /**
     * @invariant should seek to correct position before use
     */
    private fun writeBlockToFile(height: Int, hash: VbkHash, work: BigInteger, header: VeriBlockBlock) {
        check(lock.isLocked)

        blocksFile.writeInt(height)
        blocksFile.write(hash.bytes)
        blocksFile.write(Utility.toBytes(work, CHAIN_WORK_BYTES))
        blocksFile.write(header.raw)
    }

    /**
     * @invariant should seek to correct position before use
     */
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

    override fun close() {
        blocksFile.close()
    }
}
