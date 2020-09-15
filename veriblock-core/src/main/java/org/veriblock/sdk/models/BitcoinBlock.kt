// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.crypto.Sha256Hash
import org.veriblock.sdk.util.putLEBytes
import org.veriblock.sdk.util.putLEInt32
import java.nio.ByteBuffer
import java.util.Arrays

class BitcoinBlock(
    val version: Int,
    val previousBlock: Sha256Hash,
    val merkleRoot: Sha256Hash,
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

    val hash: Sha256Hash = Sha256Hash.wrapReversed(Sha256Hash.hashTwice(raw))

    fun getMerkleRootReversed(): Sha256Hash =
        Sha256Hash.wrap(merkleRoot.reversedBytes)

    override fun equals(other: Any?): Boolean {
        return this === other || other != null && javaClass == other.javaClass && Arrays.equals(
            raw, (other as BitcoinBlock).raw
        )
    }

    override fun hashCode(): Int {
        return raw.contentHashCode()
    }
}
