// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.crypto.BtcHash
import org.veriblock.core.crypto.MerkleRoot
import org.veriblock.core.crypto.asMerkleRoot
import org.veriblock.core.crypto.btcHashOf
import org.veriblock.sdk.util.putLEBytes
import org.veriblock.sdk.util.putLEInt32
import java.nio.ByteBuffer
import java.util.Arrays

class BitcoinBlock(
    val version: Int,
    val previousBlock: BtcHash,
    val merkleRoot: MerkleRoot,
    val timestamp: Int,
    val difficulty: Int,
    val nonce: Int
) {
    val raw: ByteArray = run {
        val buffer = ByteBuffer.allocateDirect(Constants.HEADER_SIZE_BitcoinBlock)
        buffer.putLEInt32(version)
        buffer.putLEBytes(previousBlock.bytes)
        buffer.putLEBytes(merkleRoot.bytes)
        buffer.putLEInt32(timestamp)
        buffer.putLEInt32(difficulty)
        buffer.putLEInt32(nonce)
        buffer.flip()
        val bytes = ByteArray(Constants.HEADER_SIZE_BitcoinBlock)
        buffer[bytes]
        bytes
    }

    val hash: BtcHash = btcHashOf(raw)

    fun getMerkleRootReversed(): MerkleRoot =
        merkleRoot.reversedBytes().asMerkleRoot()

    override fun equals(other: Any?): Boolean {
        return this === other || other != null && javaClass == other.javaClass && Arrays.equals(
            raw, (other as BitcoinBlock).raw
        )
    }

    override fun hashCode(): Int {
        return raw.contentHashCode()
    }

    override fun toString(): String {
        return "BtcBlock(hash=${hash} prev=${previousBlock} time=${timestamp})"
    }
}
