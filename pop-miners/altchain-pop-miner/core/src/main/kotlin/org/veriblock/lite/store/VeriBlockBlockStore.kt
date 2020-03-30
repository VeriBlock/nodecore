// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.lite.store

import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.models.BlockStoreException
import org.veriblock.sdk.models.Constants
import org.veriblock.sdk.models.VBlakeHash
import org.veriblock.sdk.util.ArrayUtils
import org.veriblock.sdk.util.Utils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.LinkedHashMap
import java.util.LinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val RECORD_SIZE = StoredVeriBlockBlock.SIZE

/** The default number of headers that will be stored in the ring buffer.  */
private const val DEFAULT_NUM_HEADERS = 5000

private const val FILE_PROLOGUE_BYTES = 1024
private val NOT_FOUND_MARKER = Any()

private val logger = createLogger {}

/**
 * File format:
 * 4 header bytes = "VPSV"
 * 4 cursor bytes, which indicate the offset from the first kb where the next block header should be written.
 * 24 bytes for the hash of the chain head
 * 4 bytes for the file series number
 *
 * For each header (132 bytes)
 * 24 bytes hash of the header
 * 12 bytes of chain work
 * 32 bytes hash of Bitcoin block of proof
 * 64 bytes of block header data
 */
class VeriBlockBlockStore(
    private var workingStoreFile: File,
    capacity: Int = DEFAULT_NUM_HEADERS
) {
    private val header: String = Constants.VERIBLOCK_HEADER_MAGIC

    protected var lock = ReentrantLock(true)

    /**
     * Use a separate cache to track get() misses. This is to efficiently handle the case of an unconnected block
     * during chain download. Each new block will do a get() on the unconnected block so if we haven't seen it yet we
     * must efficiently respond.
     *
     * We don't care about the value in this cache. It is always NOT_FOUND_MARKER. Unfortunately LinkedHashSet does not
     * provide the removeEldestEntry control.
     */
    protected var notFoundCache: LinkedHashMap<VBlakeHash, Any> = object : LinkedHashMap<VBlakeHash, Any>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<VBlakeHash, Any>?): Boolean {
            return size > 100  // This was chosen arbitrarily.
        }
    }
    private var storeFileChannel: FileChannel
    private var fileLock: FileLock? = null
    protected var randomAccessFile: RandomAccessFile? = null

    @Volatile
    protected var buffer: MappedByteBuffer? = null

    protected val safeBuffer: MappedByteBuffer get() = buffer ?: throw BlockStoreException("Store closed")

    protected var lastChainHead: StoredVeriBlockBlock? = null

    private var fileSeriesNumber: Int? = null

    private var capacity: Int = 0
    val recordSize: Int
        get() = RECORD_SIZE

    /** Returns the size in bytes of the file that is used to store the chain with the current parameters.  */
    fun getFileSize(): Int = FILE_PROLOGUE_BYTES + recordSize * capacity

    private fun getFileSeriesNumber(): Int {
        val buffer = this.buffer
            ?: throw BlockStoreException("Store closed")

        return lock.withLock {
            if (fileSeriesNumber == null) {
                buffer.position(40)
                fileSeriesNumber = buffer.getInt()
            }
            fileSeriesNumber!!
        }
    }

    private fun setFileSeriesNumber(value: Int) {
        val buffer = this.buffer
            ?: throw BlockStoreException("Store closed")

        lock.withLock {
            fileSeriesNumber = value
            buffer.position(40)
            buffer.putInt(fileSeriesNumber!!)
        }
    }

    /**
     * Creates and initializes an SPV block store. Will create the given file if it's missing. This operation
     * will block on disk.
     */
    init {
        require(header.length == 4) { "Header must be 4 characters long" }
        try {
            this.capacity = capacity
            val exists = workingStoreFile.exists()
            // Set up the backing file.
            this.randomAccessFile = RandomAccessFile(workingStoreFile, "rw")
            val fileSize = getFileSize().toLong()
            if (!exists) {
                logger.info("Creating new SPV block chain file $workingStoreFile")
                randomAccessFile!!.setLength(fileSize)
            } else if (randomAccessFile!!.length() != fileSize) {
                throw BlockStoreException(
                    "File size on disk does not match expected size: " +
                        randomAccessFile!!.length() + " vs " + fileSize
                )
            }

            this.storeFileChannel = randomAccessFile!!.channel
            this.fileLock = storeFileChannel.tryLock()
            if (fileLock == null)
                throw BlockStoreException("Store file is already locked by another process")

            // Map it into memory read/write. The kernel will take care of flushing writes to disk at the most
            // efficient times, which may mean that until the map is deallocated the data on disk is randomly
            // inconsistent. However the only process accessing it is us, via this mapping, so our own view will
            // always be correct. Once we establish the mmap the underlying file and storeFileChannel can go away. Note that
            // the details of mmapping vary between platforms.
            this.buffer = storeFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize)

            // Check or initialize the header bytes to ensure we don't try to open some random file.
            val headerBytes: ByteArray
            if (exists) {
                headerBytes = ByteArray(4)
                buffer!!.get(headerBytes)
                if (this.header != String(headerBytes, StandardCharsets.US_ASCII)) {
                    throw BlockStoreException("Header bytes do not equal " + this.header)
                }
            } else {
                initNewStore()
            }
        } catch (e: Exception) {
            try {
                if (randomAccessFile != null) randomAccessFile!!.close()
            } catch (e2: IOException) {
                throw BlockStoreException(e2)
            }

            throw BlockStoreException(e)
        }

    }

    @Throws(BlockStoreException::class)
    fun getChainHead(): StoredVeriBlockBlock? {
        val buffer = safeBuffer

        return lock.withLock {
            if (lastChainHead == null) {
                val headHash = ByteArray(VBlakeHash.VERIBLOCK_LENGTH)
                buffer.position(8)
                buffer.get(headHash)
                val hash = VBlakeHash.wrap(headHash)
                if (VBlakeHash.EMPTY_HASH == hash) {
                    return@withLock null
                }

                val block = get(hash)
                    ?: throw BlockStoreException("Corrupted block store: could not find chain head: $hash")
                lastChainHead = block
            }
            lastChainHead
        }
    }

    @Throws(BlockStoreException::class)
    fun setChainHead(chainHead: StoredVeriBlockBlock): StoredVeriBlockBlock? {
        val buffer = safeBuffer

        return lock.withLock {
            val previous = lastChainHead
            lastChainHead = chainHead
            val headHash = chainHead.block.hash.bytes
            buffer.position(8)
            buffer.put(headHash)

            previous
        }
    }

    /**
     * Places a new stored block into the next position in the ring buffer
     * @param storedBlock The new stored block
     * @return The value stored at the ring buffer location that is being replaced
     * @throws BlockStoreException Thrown if the ByteBuffer is null
     */
    @Throws(BlockStoreException::class)
    fun put(storedBlock: StoredVeriBlockBlock): StoredVeriBlockBlock {
        val buffer = safeBuffer

        return lock.withLock {
            var cursor = getRingCursor(buffer)
            if (cursor == getFileSize()) {
                // Wrapped around.
                archive()
                cursor = FILE_PROLOGUE_BYTES
            }
            buffer.position(cursor + VBlakeHash.VERIBLOCK_LENGTH)
            val replaced = StoredVeriBlockBlock.deserialize(buffer)

            buffer.position(cursor)
            val hash = storedBlock.block.hash
            notFoundCache.remove(hash)
            storedBlock.serialize(buffer)
            setRingCursor(buffer, buffer.position())

            replaced
        }
    }

    @Throws(BlockStoreException::class)
    fun replace(hash: VBlakeHash, storedBlock: StoredVeriBlockBlock): StoredVeriBlockBlock? {
        val buffer = safeBuffer

        return lock.withLock {
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            val targeVBlakeHashBytes = hash.bytes
            val scratch = ByteArray(targeVBlakeHashBytes.size)
            do {
                cursor -= RECORD_SIZE
                if (cursor < FILE_PROLOGUE_BYTES) {
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor)
                buffer.get(scratch)
                if (Arrays.equals(scratch, targeVBlakeHashBytes)) {
                    // Found the target.
                    val replaced = StoredVeriBlockBlock.deserialize(buffer)
                    buffer.position(cursor)
                    storedBlock.serialize(buffer)
                    return@withLock replaced
                }
            } while (cursor != startingPoint)

            return@withLock null
        }
    }

    @Throws(BlockStoreException::class)
    fun get(hash: VBlakeHash): StoredVeriBlockBlock? {
        val buffer = safeBuffer

        return lock.withLock {
            if (notFoundCache[hash] != null) {
                return@withLock null
            }

            // Starting from the current tip of the ring work backwards until we have either found the block or
            // wrapped around.
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            val targeVBlakeHashBytes = hash.bytes

            // A VeriBlock block contains partial hashes for previous blocks
            val offset = VBlakeHash.VERIBLOCK_LENGTH - targeVBlakeHashBytes.size
            val scratch = ByteArray(targeVBlakeHashBytes.size)
            do {
                cursor -= RECORD_SIZE
                if (cursor < FILE_PROLOGUE_BYTES) {
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                    // Break the loop if we had already started at the end
                    if (startingPoint == fileSize) {
                        break
                    }
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor + offset)
                buffer.get(scratch)
                if (Arrays.equals(scratch, targeVBlakeHashBytes)) {
                    // Found the target.
                    return@withLock StoredVeriBlockBlock.deserialize(buffer)
                }
            } while (cursor != startingPoint)

            notFoundCache[hash] = NOT_FOUND_MARKER
            return@withLock null
        }
    }

    @Throws(BlockStoreException::class)
    fun get(hash: VBlakeHash, count: Int): List<StoredVeriBlockBlock> {
        val buffer = safeBuffer

        if (count <= 0) {
            return emptyList()
        }

        return lock.withLock {
            if (notFoundCache[hash] != null) {
                return@withLock emptyList()
            }

            // Starting from the current tip of the ring work backwards until we have either found the block or
            // wrapped around.
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            var targeVBlakeHashBytes = hash.bytes
            var scratch = ByteArray(targeVBlakeHashBytes.size)
            var offset = VBlakeHash.VERIBLOCK_LENGTH - targeVBlakeHashBytes.size
            var foundInitial = false
            val blocks = LinkedList<StoredVeriBlockBlock>()
            do {
                cursor -= RECORD_SIZE
                if (cursor < FILE_PROLOGUE_BYTES) {
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor + offset)
                buffer.get(scratch)
                if (Arrays.equals(scratch, targeVBlakeHashBytes)) {
                    // Found the target.
                    val block = StoredVeriBlockBlock.deserialize(buffer)
                    blocks.add(block)

                    // Found the requested number.
                    if (blocks.size == count) {
                        break
                    }

                    // Look for the previous block
                    targeVBlakeHashBytes = block.block.previousBlock.bytes
                    if (!foundInitial) {
                        scratch = ByteArray(targeVBlakeHashBytes.size)
                        offset = VBlakeHash.VERIBLOCK_LENGTH - targeVBlakeHashBytes.size

                        foundInitial = true
                    }
                }
            } while (cursor != startingPoint)

            blocks
        }
    }

    fun getFromChain(hash: VBlakeHash, blocksAgo: Int): StoredVeriBlockBlock? {
        val buffer = safeBuffer

        return lock.withLock {
            if (notFoundCache[hash] != null) {
                return@withLock null
            }

            // Starting from the current tip of the ring work backwards until we have either found the block or
            // wrapped around.
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            var targeVBlakeHashBytes = hash.bytes
            var scratch = ByteArray(targeVBlakeHashBytes.size)
            var counter = 0
            do {
                cursor -= RECORD_SIZE
                if (cursor < FILE_PROLOGUE_BYTES) {
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor)
                buffer.get(scratch)
                if (Arrays.equals(scratch, targeVBlakeHashBytes)) {
                    // Found the target.
                    counter++

                    if (counter == blocksAgo) {
                        return@withLock StoredVeriBlockBlock.deserialize(buffer)
                    }

                    // Update the intermediate target with the previous block
                    buffer.position(cursor + 74)
                    targeVBlakeHashBytes = ByteArray(VBlakeHash.PREVIOUS_BLOCK_LENGTH)
                    buffer.get(targeVBlakeHashBytes)

                    if (scratch.size != targeVBlakeHashBytes.size) {
                        scratch = ByteArray(targeVBlakeHashBytes.size)
                    }
                }
            } while (cursor != startingPoint)

            null
        }
    }

    fun scanBestChain(hash: VBlakeHash): StoredVeriBlockBlock? {
        val buffer = safeBuffer

        return lock.withLock {
            // Starting from the current tip of the ring work backwards until we have either found the block or
            // wrapped around.
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            val targeVBlakeHashBytes = hash.bytes

            // A VeriBlock block contains partial hashes for previous blocks
            var intermediateTarget = getChainHead()!!.hash.trimToPreviousBlockSize().bytes
            val offset = VBlakeHash.VERIBLOCK_LENGTH - intermediateTarget.size
            val scratch = ByteArray(intermediateTarget.size)
            do {
                cursor -= RECORD_SIZE
                if (cursor < FILE_PROLOGUE_BYTES) {
                    if (startingPoint == fileSize) break
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor + offset)
                buffer.get(scratch)
                if (Arrays.equals(scratch, intermediateTarget)) {
                    if (ArrayUtils.matches(intermediateTarget, targeVBlakeHashBytes)) {
                        // Found the ACTUAL target.
                        return@withLock StoredVeriBlockBlock.deserialize(buffer)
                    }
                    // Update the intermediate target with the previous block
                    buffer.position(cursor + 42)
                    intermediateTarget = ByteArray(VBlakeHash.PREVIOUS_BLOCK_LENGTH)
                    buffer.get(intermediateTarget)
                }
            } while (cursor != startingPoint)

            null
        }
    }

    @Throws(BlockStoreException::class)
    fun close() {
        try {
            buffer!!.force()
            buffer = null  // Allow it to be GCd and the underlying file mapping to go away.
            randomAccessFile!!.close()
        } catch (e: IOException) {
            throw BlockStoreException(e)
        }

    }

    protected fun archive() {
        val seriesNumber = getFileSeriesNumber()
        val fileName = Utils.getFileNameWithoutExtension(workingStoreFile)
        val fileSeries = Utils.leftPad(Integer.toHexString(seriesNumber).toCharArray(), '0', 8)
        val archiveName = "$fileName-$fileSeries.spv"

        val archive = File(workingStoreFile.parentFile, archiveName)
        if (!archive.exists()) {
            try {
                val created = archive.createNewFile()
                if (created) {
                    logger.info { "Created VBK block store archive file: ${archive.name}" }
                }
            } catch (e: IOException) {
                logger.error(e) { "Unable to create archive file: $archiveName" }
            }

        }
        lock.withLock {
            try {
                FileInputStream(workingStoreFile).use { inputStream ->
                    FileOutputStream(archive).use { stream ->
                        fileLock!!.release()
                        val buf = ByteArray(1024)
                        var length: Int = inputStream.read(buf)
                        while (length > 0) {
                            stream.write(buf, 0, length)
                            length = inputStream.read(buf)
                        }
                        setFileSeriesNumber(seriesNumber + 1)
                    }
                }
            } catch (e: IOException) {
                logger.error(e) { "Unable to make archive: $archiveName" }
            } finally {
                try {
                    fileLock = storeFileChannel.tryLock()
                } catch (e: IOException) {
                    logger.error(e) { "Unable to reacquire file lock" }
                }
            }
        }
    }

    private fun initNewStore() {
        val headerBytes: ByteArray = header.toByteArray(StandardCharsets.US_ASCII)

        lock.withLock {
            buffer!!.put(headerBytes)
            setFileSeriesNumber(0)
            setRingCursor(buffer!!, FILE_PROLOGUE_BYTES)
        }
    }

    /** Returns the offset from the file start where the latest block should be written (end of prev block).  */
    protected fun getRingCursor(buffer: ByteBuffer): Int {
        val c = buffer.getInt(4)
        check(c >= FILE_PROLOGUE_BYTES) { "The ring cursor must be farther than $FILE_PROLOGUE_BYTES, but it is $c!" }
        return c
    }

    protected fun setRingCursor(buffer: ByteBuffer, newCursor: Int) {
        require(newCursor >= 0) { "The ring cursor must be positive!" }
        buffer.putInt(4, newCursor)
    }

    fun reset() {
        val headerBytes: ByteArray = header.toByteArray(StandardCharsets.US_ASCII)

        lock.withLock {
            buffer!!.position(0)
            buffer!!.put(headerBytes)
            setFileSeriesNumber(0)
            setRingCursor(buffer!!, FILE_PROLOGUE_BYTES)
            buffer!!.position(8)
            buffer!!.put(VBlakeHash.EMPTY_HASH.bytes)
            lastChainHead = null
        }
    }
}
