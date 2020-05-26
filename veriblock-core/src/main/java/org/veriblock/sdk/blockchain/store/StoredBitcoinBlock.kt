// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.blockchain.store

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.utilities.Preconditions
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.BitcoinBlock
import org.veriblock.sdk.models.Constants
import org.veriblock.sdk.services.SerializeDeserializeService
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.Objects

class StoredBitcoinBlock(
    val block: BitcoinBlock,
    val work: BigInteger,
    val height: Int
) {
    val hash: Sha256Hash = block.hash

    init {
        require(work >= BigInteger.ZERO) {
            "Work must be positive"
        }
        require(height >= 0) {
            "Block index must be positive"
        }
    }

    fun serialize(buffer: ByteBuffer) {
        buffer.put(hash.bytes)
        buffer.putInt(height)
        buffer.put(Utility.toBytes(work, CHAIN_WORK_BYTES))
        buffer.put(SerializeDeserializeService.getHeaderBytesBitcoinBlock(block))
    }

    fun serialize(): ByteArray {
        val local = ByteBuffer.allocateDirect(SIZE)
        serialize(local)
        local.flip()
        val serialized = ByteArray(SIZE)
        local[serialized]
        return serialized
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as StoredBitcoinBlock
        return height == that.height &&
            hash == that.hash &&
            block == that.block &&
            work == that.work
    }

    override fun hashCode(): Int {
        return Objects.hash(hash, block, height, work)
    }

    companion object {
        const val SIZE = 32 + 4 + 12 + 80
        const val CHAIN_WORK_BYTES = 12
        @JvmStatic
        fun deserializeWithoutHash(buffer: ByteBuffer): StoredBitcoinBlock {
            //Skip Hash
            buffer.position(buffer.position() + Sha256Hash.BITCOIN_LENGTH)
            return deserialize(buffer)
        }

        fun deserialize(buffer: ByteBuffer): StoredBitcoinBlock {
            val index = buffer.int
            val workBytes = ByteArray(CHAIN_WORK_BYTES)
            buffer[workBytes]
            val work = BigInteger(1, workBytes)
            val blockBytes = ByteArray(Constants.HEADER_SIZE_BitcoinBlock)
            buffer[blockBytes]
            val block = SerializeDeserializeService.parseBitcoinBlock(blockBytes)
            return StoredBitcoinBlock(block, work, index)
        }

        @JvmStatic
        fun deserialize(bytes: ByteArray): StoredBitcoinBlock {
            Preconditions.notNull(bytes, "Raw Bitcoin Block cannot be null")
            Preconditions.argument<Any>(
                bytes.size >= SIZE
            ) { "Invalid raw Bitcoin Block: " + Utility.bytesToHex(bytes) }
            val local = ByteBuffer.allocateDirect(SIZE)
            local.put(bytes, bytes.size - SIZE, SIZE)
            local.flip()
            local.position(Sha256Hash.BITCOIN_LENGTH)
            return deserialize(local)
        }
    }
}
