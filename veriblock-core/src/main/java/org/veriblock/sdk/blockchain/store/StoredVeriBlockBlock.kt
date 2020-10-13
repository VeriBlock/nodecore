// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.blockchain.store

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.Preconditions
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.Constants
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.Objects

class StoredVeriBlockBlock
@JvmOverloads constructor(
    val block: VeriBlockBlock,
    val work: BigInteger,
    val hash: VBlakeHash
) {
    val height: Int
        get() = block.height

    init {
        require(work >= BigInteger.ZERO) {
            "Work must be positive"
        }
    }

    fun serialize(buffer: ByteBuffer) {
        buffer.put(hash.bytes)
        buffer.put(
            Utility.toBytes(work, CHAIN_WORK_BYTES)
        )
        buffer.put(SerializeDeserializeService.serializeHeaders(block))
    }

    fun serialize(): ByteArray {
        val local = ByteBuffer.allocateDirect(SIZE)
        serialize(local)
        local.flip()
        val serialized = ByteArray(SIZE)
        local[serialized]
        return serialized
    }

    fun getKeystoneIndex(): Int = block.height / 20 * 20

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as StoredVeriBlockBlock
        return hash == that.hash &&
            block == that.block &&
            work == that.work
    }

    override fun hashCode(): Int {
        return Objects.hash(hash, block, work)
    }

    companion object {
        const val SIZE = 24 + 64 + 12
        const val CHAIN_WORK_BYTES = 12

        @JvmStatic
        fun deserialize(buffer: ByteBuffer): StoredVeriBlockBlock {
            val hashBytes = ByteArray(VBlakeHash.VERIBLOCK_LENGTH)
            buffer.get(hashBytes)
            val hash = VBlakeHash.wrap(hashBytes)
            val workBytes = ByteArray(CHAIN_WORK_BYTES)
            buffer.get(workBytes)
            val work = BigInteger(1, workBytes)
            val blockBytes = BlockUtility.getBlockHeader(buffer)
            val block = SerializeDeserializeService.parseVeriBlockBlock(blockBytes)
            return StoredVeriBlockBlock(block, work, hash)
        }

        @JvmStatic
        fun deserialize(bytes: ByteArray): StoredVeriBlockBlock {
            Preconditions.notNull(bytes, "Raw VeriBlock Block cannot be null")
            Preconditions.argument<Any>(bytes.size >= SIZE) {
                "Invalid raw VeriBlock Block: " + Utility.bytesToHex(bytes)
            }
            val local = ByteBuffer.allocateDirect(SIZE)
            local.put(bytes, bytes.size - SIZE, SIZE)
            local.flip()
            return deserialize(local)
        }
    }
}
