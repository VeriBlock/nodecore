// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.lite.store

import org.veriblock.sdk.BitcoinBlock
import org.veriblock.sdk.Constants
import org.veriblock.sdk.Sha256Hash
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.sdk.util.Preconditions
import org.veriblock.sdk.util.Utils

import java.math.BigInteger
import java.nio.ByteBuffer

class StoredBitcoinBlock(
    val block: BitcoinBlock,
    val work: BigInteger,
    val height: Int
) {
    val hash: Sha256Hash

    init {
        Preconditions.notNull(block, "Block cannot be null!")
        Preconditions.notNull(work, "Work cannot be null!")
        Preconditions.argument<Any>(work.compareTo(BigInteger.ZERO) != -1, "Work must be positive!")
        Preconditions.argument<Any>(height >= 0, "Block index must be positive!")

        this.hash = block.hash
    }

    fun serialize(buffer: ByteBuffer) {
        buffer.put(hash.bytes)
        buffer.putInt(height)
        buffer.put(Utils.toBytes(work, CHAIN_WORK_BYTES))
        buffer.put(SerializeDeserializeService.serialize(block))
    }

    fun serialize(): ByteArray {
        val local = ByteBuffer.allocateDirect(SIZE)
        serialize(local)

        local.flip()
        val serialized = ByteArray(SIZE)
        local.get(serialized)

        return serialized
    }

    companion object {
        const val SIZE = 32 + 4 + 12 + 80
        const val CHAIN_WORK_BYTES = 12

        fun deserialize(buffer: ByteBuffer): StoredBitcoinBlock {
            val index = buffer.int

            val workBytes = ByteArray(CHAIN_WORK_BYTES)
            buffer.get(workBytes)
            val work = BigInteger(1, workBytes)

            val blockBytes = ByteArray(Constants.HEADER_SIZE_BitcoinBlock)
            buffer.get(blockBytes)
            val block = SerializeDeserializeService.parseBitcoinBlock(blockBytes)

            return StoredBitcoinBlock(block, work, index)
        }

        fun deserialize(bytes: ByteArray?): StoredBitcoinBlock {
            Preconditions.argument<Any>(bytes != null && bytes.size >= SIZE, "Bytes must at least $SIZE bytes long!")

            val local = ByteBuffer.allocateDirect(SIZE)
            local.put(bytes!!, bytes.size - SIZE, SIZE)

            local.flip()
            local.position(Sha256Hash.BITCOIN_LENGTH)

            return deserialize(local)
        }
    }
}
