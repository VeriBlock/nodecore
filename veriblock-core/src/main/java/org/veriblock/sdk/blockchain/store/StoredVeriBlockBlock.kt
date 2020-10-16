// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.blockchain.store

import org.veriblock.core.crypto.VbkHash
import org.veriblock.core.crypto.readVbkHash
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.Objects

class StoredVeriBlockBlock(
    val header: VeriBlockBlock,
    val work: BigInteger,
    val hash: VbkHash
) {
    val height: Int
        get() = header.height

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
        buffer.put(SerializeDeserializeService.serializeHeaders(header))
    }

    fun serialize(): ByteArray {
        val local = ByteBuffer.allocateDirect(SIZE)
        serialize(local)
        local.flip()
        val serialized = ByteArray(SIZE)
        local[serialized]
        return serialized
    }

    fun getKeystoneIndex(): Int = header.height / 20 * 20

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as StoredVeriBlockBlock
        return hash == that.hash &&
            header == that.header &&
            work == that.work
    }

    override fun hashCode(): Int {
        return Objects.hash(hash, header, work)
    }

    companion object {
        const val SIZE = 24 + 64 + 12
        const val CHAIN_WORK_BYTES = 12

        @JvmStatic
        fun deserialize(buffer: ByteBuffer): StoredVeriBlockBlock {
            val hash = buffer.readVbkHash()
            val workBytes = ByteArray(CHAIN_WORK_BYTES)
            buffer.get(workBytes)
            val work = BigInteger(1, workBytes)
            val blockBytes = BlockUtility.getBlockHeader(buffer)
            val block = SerializeDeserializeService.parseVeriBlockBlock(blockBytes)
            return StoredVeriBlockBlock(block, work, hash)
        }

        @JvmStatic
        fun deserialize(bytes: ByteArray): StoredVeriBlockBlock {
            check(bytes.size >= SIZE) {
                "Invalid raw VeriBlock Block: " + Utility.bytesToHex(bytes)
            }
            val local = ByteBuffer.allocateDirect(SIZE)
            local.put(bytes, bytes.size - SIZE, SIZE)
            local.flip()
            return deserialize(local)
        }
    }
}
