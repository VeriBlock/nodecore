// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.store

import org.veriblock.lite.util.invoke
import org.veriblock.sdk.BlockStoreException
import org.veriblock.sdk.createLogger
import org.veriblock.sdk.util.Preconditions
import org.veriblock.sdk.util.Utils
import java.io.*
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.locks.ReentrantLock

private val logger = createLogger {}

abstract class FlatFileBlockStore<THash, TBlock> @Throws(BlockStoreException::class) constructor(
    // Used to stop other applications/processes from opening the store.
    protected var workingStoreFile: File,
    capacity: Int,
    private val header: String
) : BlockStore<THash, TBlock> {

    protected var lock = ReentrantLock(true)
    /**
     * Use a separate cache to track get() misses. This is to efficiently handle the case of an unconnected block
     * during chain download. Each new block will do a get() on the unconnected block so if we haven't seen it yet we
     * must efficiently respond.
     *
     * We don't care about the value in this cache. It is always NOT_FOUND_MARKER. Unfortunately LinkedHashSet does not
     * provide the removeEldestEntry control.
     */
    protected var notFoundCache: LinkedHashMap<THash, Any> = object : LinkedHashMap<THash, Any>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<THash, Any>?): Boolean {
            return size > 100  // This was chosen arbitrarily.
        }
    }
    protected var storeFileChannel: FileChannel
    protected var fileLock: FileLock? = null
    protected var randomAccessFile: RandomAccessFile? = null
    @Volatile
    protected var buffer: MappedByteBuffer? = null

    protected var lastChainHead: TBlock? = null

    protected var fileSeriesNumber: Int? = null

    var capacity: Int = 0
        protected set

    protected abstract val recordSize: Int

    /** Returns the size in bytes of the file that is used to store the chain with the current parameters.  */
    fun getFileSize(): Int = FILE_PROLOGUE_BYTES + recordSize * capacity

    protected fun getFileSeriesNumber(): Int {
        val buffer = this.buffer
            ?: throw BlockStoreException("Store closed")

        return lock {
            if (fileSeriesNumber == null) {
                buffer.position(40)
                fileSeriesNumber = buffer.getInt()
            }
            fileSeriesNumber!!
        }
    }

    protected fun setFileSeriesNumber(value: Int) {
        val buffer = this.buffer
            ?: throw BlockStoreException("Store closed")

        lock.lock()
        try {
            fileSeriesNumber = value
            buffer.position(40)
            buffer.putInt(fileSeriesNumber!!)
        } finally {
            lock.unlock()
        }
    }

    /**
     * Creates and initializes an SPV block store. Will create the given file if it's missing. This operation
     * will block on disk.
     */
    init {
        Preconditions.notNull(workingStoreFile, "File cannot be null!")
        Preconditions.argument<Any>(header.length == 4, "Header must be 4 characters long")
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
                throw BlockStoreException("File size on disk does not match expected size: " +
                    randomAccessFile!!.length() + " vs " + fileSize)
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
    abstract override fun getChainHead(): TBlock?

    @Throws(BlockStoreException::class)
    abstract override fun setChainHead(chainHead: TBlock): TBlock?

    @Throws(BlockStoreException::class)
    abstract override fun put(storedBlock: TBlock): TBlock

    @Throws(BlockStoreException::class)
    abstract override fun replace(hash: THash, storedBlock: TBlock): TBlock?

    @Throws(BlockStoreException::class)
    abstract override operator fun get(hash: THash): TBlock?

    @Throws(BlockStoreException::class)
    abstract override operator fun get(hash: THash, count: Int): List<TBlock>

    @Throws(BlockStoreException::class)
    abstract override fun getFromChain(hash: THash, blocksAgo: Int): TBlock?

    @Throws(BlockStoreException::class)
    abstract override fun scanBestChain(hash: THash): TBlock?

    @Throws(BlockStoreException::class)
    open fun close() {
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
                    logger.info {"Created bitcoin block store archive file: ${archive.name}" }
                }
            } catch (e: IOException) {
                logger.error(e) { "Unable to create archive file: $archiveName" }
            }

        }
        lock {
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

        lock {
            buffer!!.put(headerBytes)
            setFileSeriesNumber(0)
            setRingCursor(buffer!!, FILE_PROLOGUE_BYTES)
        }
    }

    /** Returns the offset from the file start where the latest block should be written (end of prev block).  */
    protected fun getRingCursor(buffer: ByteBuffer): Int {
        val c = buffer.getInt(4)
        Preconditions.state(c >= FILE_PROLOGUE_BYTES, "The ring cursor must be farther than $FILE_PROLOGUE_BYTES, but it is $c!")
        return c
    }

    protected fun setRingCursor(buffer: ByteBuffer, newCursor: Int) {
        Preconditions.argument<Any>(newCursor >= 0, "The ring cursor must be positive!")
        buffer.putInt(4, newCursor)
    }

    companion object {
        val FILE_PROLOGUE_BYTES = 1024
        val NOT_FOUND_MARKER = Any()
    }
}
