// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model

import org.veriblock.core.utilities.BlockUtility
import org.veriblock.core.utilities.extensions.toHex
import java.util.Arrays

class VeriBlockHeader(
    private val bytes: ByteArray
) {
    val hash: String = BlockUtility.hashBlock(bytes)

    fun getHeight(): Int =
        BlockUtility.extractBlockHeightFromBlockHeader(bytes)

    fun getVersion(): Short =
        BlockUtility.extractVersionFromBlockHeader(bytes)

    fun getPreviousHash(): String =
        BlockUtility.extractPreviousBlockHashFromBlockHeader(bytes).toHex()

    fun getSecondPreviousHash(): String =
        BlockUtility.extractSecondPreviousBlockHashFromBlockHeader(bytes).toHex()

    fun getThirdPreviousHash(): String =
        BlockUtility.extractThirdPreviousBlockHashFromBlockHeader(bytes).toHex()

    fun getMerkleRoot(): String =
        BlockUtility.extractMerkleRootFromBlockHeader(bytes).toHex()

    fun getTimestamp(): Int =
        BlockUtility.extractTimestampFromBlockHeader(bytes)

    fun getDifficulty(): Int =
        BlockUtility.extractDifficultyFromBlockHeader(bytes)

    fun getNonce(): Int =
        BlockUtility.extractNonceFromBlockHeader(bytes)

    fun toByteArray(): ByteArray {
        return bytes
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is VeriBlockHeader) {
            return false
        }
        return Arrays.equals(bytes, other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}
