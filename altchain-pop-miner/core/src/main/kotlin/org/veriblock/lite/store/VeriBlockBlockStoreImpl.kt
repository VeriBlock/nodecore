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
import org.veriblock.sdk.Constants
import org.veriblock.sdk.VBlakeHash
import org.veriblock.sdk.util.ArrayUtils
import java.io.File
import java.util.*

private const val RECORD_SIZE = StoredVeriBlockBlock.SIZE
/** The default number of headers that will be stored in the ring buffer.  */
private const val DEFAULT_NUM_HEADERS = 5000

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
class VeriBlockBlockStoreImpl(
    file: File,
    capacity: Int = DEFAULT_NUM_HEADERS
) : FlatFileBlockStore<VBlakeHash, StoredVeriBlockBlock>(
    file,
    capacity,
    Constants.VERIBLOCK_HEADER_MAGIC
) {
    override val recordSize: Int
        get() = RECORD_SIZE

    @Throws(BlockStoreException::class)
    override fun getChainHead(): StoredVeriBlockBlock? {
        val buffer = safeBuffer

        return lock {
            if (lastChainHead == null) {
                val headHash = ByteArray(VBlakeHash.VERIBLOCK_LENGTH)
                buffer.position(8)
                buffer.get(headHash)
                val hash = VBlakeHash.wrap(headHash)
                if (VBlakeHash.EMPTY_HASH == hash) {
                    return@lock null
                }

                val block = get(hash)
                    ?: throw BlockStoreException("Corrupted block store: could not find chain head: $hash")
                lastChainHead = block
            }
            lastChainHead
        }
    }

    @Throws(BlockStoreException::class)
    override fun setChainHead(chainHead: StoredVeriBlockBlock): StoredVeriBlockBlock? {
        val buffer = safeBuffer

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
    override fun put(storedBlock: StoredVeriBlockBlock): StoredVeriBlockBlock {
        val buffer = safeBuffer

        return lock {
            var cursor = getRingCursor(buffer)
            if (cursor == getFileSize()) {
                // Wrapped around.
                archive()
                cursor = FlatFileBlockStore.FILE_PROLOGUE_BYTES
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
    override fun replace(hash: VBlakeHash, storedBlock: StoredVeriBlockBlock): StoredVeriBlockBlock? {
        val buffer = safeBuffer

        return lock {
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            val targetHashBytes = hash.bytes
            val scratch = ByteArray(targetHashBytes.size)
            do {
                cursor -= RECORD_SIZE
                if (cursor < FlatFileBlockStore.FILE_PROLOGUE_BYTES) {
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor)
                buffer.get(scratch)
                if (Arrays.equals(scratch, targetHashBytes)) {
                    // Found the target.
                    val replaced = StoredVeriBlockBlock.deserialize(buffer)
                    buffer.position(cursor)
                    storedBlock.serialize(buffer)
                    return@lock replaced
                }
            } while (cursor != startingPoint)

            return@lock null
        }
    }

    @Throws(BlockStoreException::class)
    override fun get(hash: VBlakeHash): StoredVeriBlockBlock? {
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

            // A VeriBlock block contains partial hashes for previous blocks
            val offset = VBlakeHash.VERIBLOCK_LENGTH - targetHashBytes.size
            val scratch = ByteArray(targetHashBytes.size)
            do {
                cursor -= RECORD_SIZE
                if (cursor < FlatFileBlockStore.FILE_PROLOGUE_BYTES) {
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor + offset)
                buffer.get(scratch)
                if (Arrays.equals(scratch, targetHashBytes)) {
                    // Found the target.
                    return@lock StoredVeriBlockBlock.deserialize(buffer)
                }
            } while (cursor != startingPoint)

            notFoundCache[hash] = NOT_FOUND_MARKER
            return@lock null
        }
    }

    @Throws(BlockStoreException::class)
    override fun get(hash: VBlakeHash, count: Int): List<StoredVeriBlockBlock> {
        val buffer = safeBuffer

        if (count <= 0) {
            return emptyList()
        }

        return lock {
            if (notFoundCache[hash] != null) {
                return@lock emptyList()
            }

            // Starting from the current tip of the ring work backwards until we have either found the block or
            // wrapped around.
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            var targetHashBytes = hash.bytes
            var scratch = ByteArray(targetHashBytes.size)
            var offset = VBlakeHash.VERIBLOCK_LENGTH - targetHashBytes.size
            var foundInitial = false
            val blocks = LinkedList<StoredVeriBlockBlock>()
            do {
                cursor -= RECORD_SIZE
                if (cursor < FlatFileBlockStore.FILE_PROLOGUE_BYTES) {
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor + offset)
                buffer.get(scratch)
                if (Arrays.equals(scratch, targetHashBytes)) {
                    // Found the target.
                    val block = StoredVeriBlockBlock.deserialize(buffer)
                    blocks.add(block)

                    // Found the requested number.
                    if (blocks.size == count) {
                        break
                    }

                    // Look for the previous block
                    targetHashBytes = block.block.previousBlock.bytes
                    if (!foundInitial) {
                        scratch = ByteArray(targetHashBytes.size)
                        offset = VBlakeHash.VERIBLOCK_LENGTH - targetHashBytes.size

                        foundInitial = true
                    }
                }
            } while (cursor != startingPoint)

            blocks
        }
    }

    override fun getFromChain(hash: VBlakeHash, blocksAgo: Int): StoredVeriBlockBlock? {
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
            var scratch = ByteArray(targetHashBytes.size)
            var counter = 0
            do {
                cursor -= RECORD_SIZE
                if (cursor < FlatFileBlockStore.FILE_PROLOGUE_BYTES) {
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
                        return@lock StoredVeriBlockBlock.deserialize(buffer)
                    }

                    // Update the intermediate target with the previous block
                    buffer.position(cursor + 74)
                    targetHashBytes = ByteArray(VBlakeHash.PREVIOUS_BLOCK_LENGTH)
                    buffer.get(targetHashBytes)

                    if (scratch.size != targetHashBytes.size) {
                        scratch = ByteArray(targetHashBytes.size)
                    }
                }
            } while (cursor != startingPoint)

            null
        }
    }

    override fun scanBestChain(hash: VBlakeHash): StoredVeriBlockBlock? {
        val buffer = safeBuffer

        return lock {
            // Starting from the current tip of the ring work backwards until we have either found the block or
            // wrapped around.
            var cursor = getRingCursor(buffer)
            val startingPoint = cursor
            val fileSize = getFileSize()
            val targetHashBytes = hash.bytes

            // A VeriBlock block contains partial hashes for previous blocks
            var intermediateTarget = getChainHead()!!.hash.trimToPreviousBlockSize().bytes
            val offset = VBlakeHash.VERIBLOCK_LENGTH - intermediateTarget.size
            val scratch = ByteArray(intermediateTarget.size)
            do {
                cursor -= RECORD_SIZE
                if (cursor < FlatFileBlockStore.Companion.FILE_PROLOGUE_BYTES) {
                    if (startingPoint == fileSize) break
                    // We hit the start, so wrap around.
                    cursor = fileSize - RECORD_SIZE
                }
                // Cursor is now at the start of the next record to check, so read the hash and compare it.
                buffer.position(cursor + offset)
                buffer.get(scratch)
                if (Arrays.equals(scratch, intermediateTarget)) {
                    if (ArrayUtils.matches(intermediateTarget, targetHashBytes)) {
                        // Found the ACTUAL target.
                        return@lock StoredVeriBlockBlock.deserialize(buffer)
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
}
