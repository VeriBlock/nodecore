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

class StoredVeriBlockBlock @JvmOverloads constructor(
    val block: VeriBlockBlock,
    val work: BigInteger,
    private var blockOfProof: Sha256Hash = Sha256Hash.ZERO_HASH
) {
    val hash: VBlakeHash = block.hash

    val height: Int
        get() = block.height

    init {
        require (work >= BigInteger.ZERO) {
            "Work must be positive"
        }
    }

    fun getBlockOfProof(): Sha256Hash {
        return blockOfProof
    }

    fun setBlockOfProof(blockOfProof: Sha256Hash) {
        Preconditions.notNull(blockOfProof, "Block of proof cannot be null")
        Preconditions.argument<Any>(
            blockOfProof.length == Sha256Hash.BITCOIN_LENGTH
        ) { "Invalid block of proof: $blockOfProof" }
        this.blockOfProof = blockOfProof
    }

    fun serialize(buffer: ByteBuffer) {
        buffer.put(hash.bytes)
        buffer.put(
            Utility.toBytes(work, CHAIN_WORK_BYTES)
        )
        buffer.put(blockOfProof.bytes)
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
            work == that.work &&
            blockOfProof == that.blockOfProof
    }

    override fun hashCode(): Int {
        return Objects.hash(hash, block, work, blockOfProof)
    }

    companion object {
        const val SIZE = 24 + 12 + 32 + 64
        const val CHAIN_WORK_BYTES = 12

        @JvmStatic
        fun deserialize(buffer: ByteBuffer): StoredVeriBlockBlock {
            val workBytes = ByteArray(CHAIN_WORK_BYTES)
            buffer.get(workBytes)
            val work = BigInteger(1, workBytes)
            val blockOfProofBytes = ByteArray(Sha256Hash.BITCOIN_LENGTH)
            buffer.get(blockOfProofBytes)
            val blockOfProof = Sha256Hash.wrap(blockOfProofBytes)
            val blockBytes = BlockUtility.getBlockHeader(buffer)
            val block = SerializeDeserializeService.parseVeriBlockBlock(blockBytes)
            return StoredVeriBlockBlock(block, work, blockOfProof)
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
            local.position(VBlakeHash.VERIBLOCK_LENGTH)
            return deserialize(local)
        }
    }
}
