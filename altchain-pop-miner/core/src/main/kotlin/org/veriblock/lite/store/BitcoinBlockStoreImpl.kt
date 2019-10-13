// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

// This is adapted from org.bitcoinj.store.SPVBlockStore in bitcoinj-core version 0.14.7
// which is licensed as follows:
package org.veriblock.lite.store

import org.veriblock.lite.util.invoke
import org.veriblock.sdk.BlockStoreException
import org.veriblock.sdk.Constants
import org.veriblock.sdk.Sha256Hash
import org.veriblock.sdk.util.Utils
import java.io.File
import java.io.IOException
import java.util.*

private const val RECORD_SIZE = StoredBitcoinBlock.SIZE
/** The default number of headers that will be stored in the ring buffer.  */
private const val DEFAULT_NUM_HEADERS = 5000

/**
 * File format:
 * 4 header bytes = "SPVB"
 * 4 cursor bytes, which indicate the offset from the first kb where the next block header should be written.
 * 32 bytes for the hash of the chain head
 * 4 bytes for the file series number
 *
 * For each header (128 bytes)
 * 32 bytes hash of the header
 * 4 bytes of height
 * 12 bytes of chain work
 * 80 bytes of block header data
 */
class BitcoinBlockStoreImpl(
    file: File,
    capacity: Int = DEFAULT_NUM_HEADERS
) : FlatFileBlockStore<Sha256Hash, StoredBitcoinBlock>(
    file,
    capacity,
    Constants.BITCOIN_HEADER_MAGIC
) {
    override val recordSize: Int
        get() = RECORD_SIZE

    @Throws(BlockStoreException::class)
    override fun getChainHead(): StoredBitcoinBlock? {
        val buffer = safeBuffer

        return lock {
            if (lastChainHead == null) {
                val headHash = ByteArray(Sha256Hash.BITCOIN_LENGTH)
                buffer.position(8)
                buffer.get(headHash)
                val hash = Sha256Hash.wrap(headHash)
                val block = get(hash) ?: throw BlockStoreException("Corrupted block store: could not find chain head: $hash")
                lastChainHead = block
            }
            lastChainHead
        }
    }

    @Throws(BlockStoreException::class)
    override fun setChainHead(chainHead: StoredBitcoinBlock): StoredBitcoinBlock? {
        val buffer = this.buffer
            ?: throw BlockStoreException("Store closed")

        return lock {
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
    override fun put(storedBlock: StoredBitcoinBlock): StoredBitcoinBlock {
        val buffer = safeBuffer

        return lock {
            var cursor = getRingCursor(buffer)
            if (cursor == getFileSize()) {
                // Wrapped around.
                archive()
                cursor = FILE_PROLOGUE_BYTES
            }
            buffer.position(cursor + Sha256Hash.BITCOIN_LENGTH)
            val replaced = StoredBitcoinBlock.deserialize(buffer)

            buffer.position(cursor)
            val hash = storedBlock.block.hash
            notFoundCache.remove(hash)
            storedBlock.serialize(buffer)
            setRingCursor(buffer, buffer.position())

            replaced
        }
    }

    @Throws(BlockStoreException::class)
    override fun replace(hash: Sha256Hash, storedBlock: StoredBitcoinBlock): StoredBitcoinBlock? {
        val buffer = safeBuffer

        return lock {
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            val targetHashBytes = hash.bytes
            val scratch = ByteArray(targetHashBytes.size)
            do {
                cursor -= RECORD_SIZE
                if (cursor < FILE_PROLOGUE_BYTES) {
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor)
                buffer.get(scratch)
                if (Arrays.equals(scratch, targetHashBytes)) {
                    // Found the target.
                    val replaced = StoredBitcoinBlock.deserialize(buffer)
                    buffer.position(cursor)
                    storedBlock.serialize(buffer)
                    return@lock replaced
                }
            } while (cursor != startingPoint)

            null
        }
    }

    @Throws(BlockStoreException::class)
    override fun get(hash: Sha256Hash): StoredBitcoinBlock? {
        val buffer = safeBuffer

        return lock {
            if (notFoundCache[hash] != null) {
                return@lock null
            }

            // Starting from the current tip of the ring work backwards until we have either found the block or
            // wrapped around.
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            val targetHashBytes = hash.bytes
            val scratch = ByteArray(Sha256Hash.BITCOIN_LENGTH)
            do {
                cursor -= RECORD_SIZE
                if (cursor < FILE_PROLOGUE_BYTES) {
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor)
                buffer.get(scratch)
                if (Arrays.equals(scratch, targetHashBytes)) {
                    // Found the target.
                    return@lock StoredBitcoinBlock.deserialize(buffer)
                }
            } while (cursor != startingPoint)

            notFoundCache[hash] = NOT_FOUND_MARKER
            null
        }
    }

    @Throws(BlockStoreException::class)
    override fun get(hash: Sha256Hash, count: Int): List<StoredBitcoinBlock> {
        val buffer = safeBuffer

        val blocks = LinkedList<StoredBitcoinBlock>()
        if (count <= 0) return blocks

        return lock {
            if (notFoundCache[hash] != null) {
                return@lock blocks
            }

            // Starting from the current tip of the ring work backwards until we have either found the block or
            // wrapped around.
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            var targetHashBytes = hash.bytes
            val scratch = ByteArray(Sha256Hash.BITCOIN_LENGTH)
            do {
                cursor -= RECORD_SIZE
                if (cursor < FILE_PROLOGUE_BYTES) {
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor)
                buffer.get(scratch)
                if (Arrays.equals(scratch, targetHashBytes)) {
                    // Found the target.
                    val block = StoredBitcoinBlock.deserialize(buffer)
                    blocks.add(block)

                    // Found the requested number.
                    if (blocks.size == count) {
                        break
                    }

                    // Look for the previous block
                    targetHashBytes = block.block.previousBlock.bytes
                }
            } while (cursor != startingPoint)

            blocks
        }
    }

    override fun getFromChain(hash: Sha256Hash, blocksAgo: Int): StoredBitcoinBlock? {
        val buffer = safeBuffer

        return lock {
            if (notFoundCache[hash] != null) {
                return@lock null
            }

            // Starting from the current tip of the ring work backwards until we have either found the block or
            // wrapped around.
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            var targetHashBytes = hash.bytes
            val scratch = ByteArray(Sha256Hash.BITCOIN_LENGTH)
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
                if (Arrays.equals(scratch, targetHashBytes)) {
                    // Found the target.
                    counter++

                    if (counter == blocksAgo) {
                        return@lock StoredBitcoinBlock.deserialize(buffer)
                    }

                    // Update the intermediate target with the previous block
                    buffer.position(cursor + 52)
                    val previous = ByteArray(Sha256Hash.BITCOIN_LENGTH)
                    buffer.get(previous)
                    targetHashBytes = Utils.reverseBytes(previous)
                }
            } while (cursor != startingPoint)

            null
        }
    }

    override fun scanBestChain(hash: Sha256Hash): StoredBitcoinBlock? {
        val buffer = safeBuffer

        return lock {
            // Starting from the current tip of the ring work backwards until we have either found the block or
            // wrapped around.
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            val targetHashBytes = hash.bytes
            var intermediateTarget = getChainHead()!!.hash.bytes
            val scratch = ByteArray(Sha256Hash.BITCOIN_LENGTH)
            do {
                cursor -= RECORD_SIZE
                if (cursor < FILE_PROLOGUE_BYTES) {
                    if (startingPoint == fileSize) break
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor)
                buffer.get(scratch)
                if (Arrays.equals(scratch, intermediateTarget)) {
                    if (Arrays.equals(intermediateTarget, targetHashBytes)) {
                        // Found the ACTUAL target.
                        return@lock StoredBitcoinBlock.deserialize(buffer)
                    }
                    // Update the intermediate target with the previous block
                    buffer.position(cursor + 52)
                    val previous = ByteArray(Sha256Hash.BITCOIN_LENGTH)
                    buffer.get(previous)
                    intermediateTarget = Utils.reverseBytes(previous)
                }
            } while (cursor != startingPoint)

            null
        }
    }

    @Throws(BlockStoreException::class)
    override fun close() {
        try {
            buffer!!.force()
            buffer = null  // Allow it to be GCd and the underlying file mapping to go away.
            randomAccessFile!!.close()
        } catch (e: IOException) {
            throw BlockStoreException(e)
        }
    }
}
