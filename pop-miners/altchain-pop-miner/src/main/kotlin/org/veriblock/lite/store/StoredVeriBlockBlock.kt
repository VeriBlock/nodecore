// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.lite.store

import org.veriblock.sdk.models.Constants
import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.core.crypto.VBlakeHash
import org.veriblock.core.utilities.BlockUtility
import org.veriblock.sdk.models.VeriBlockBlock
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.core.utilities.Utility
import java.math.BigInteger
import java.nio.ByteBuffer

class StoredVeriBlockBlock(
    val block: VeriBlockBlock,
    val work: BigInteger,
    private var blockOfProof: Sha256Hash = Sha256Hash.ZERO_HASH
) {
    val hash: VBlakeHash

    val height: Int
        get() = block.height

    val keystoneIndex: Int
        get() = block.height / 20 * 20

    init {
        require(work >= BigInteger.ZERO) { "Work must be positive!" }
        this.hash = block.hash
    }

    fun getBlockOfProof(): Sha256Hash? {
        return blockOfProof
    }

    fun setBlockOfProof(blockOfProof: Sha256Hash) {
        require(blockOfProof.length == Sha256Hash.BITCOIN_LENGTH) { "Invalid block of proof $blockOfProof" }
        this.blockOfProof = blockOfProof
    }

    fun serialize(buffer: ByteBuffer) {
        val initialPosition = buffer.position()
        buffer.put(hash.bytes)
        buffer.put(Utility.toBytes(work, CHAIN_WORK_BYTES))
        buffer.put(blockOfProof.bytes)
        buffer.put(SerializeDeserializeService.serializeHeaders(block))
        buffer.position(initialPosition + SIZE)
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
        const val SIZE = 24 + 12 + 32 + 65
        const val CHAIN_WORK_BYTES = 12

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
    }
}
