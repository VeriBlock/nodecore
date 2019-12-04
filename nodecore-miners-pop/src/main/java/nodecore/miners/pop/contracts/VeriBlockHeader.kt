// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.contracts

import nodecore.miners.pop.common.Utility
import org.veriblock.core.utilities.BlockUtility
import java.util.*

class VeriBlockHeader(private val bytes: ByteArray) {
    val hash: String = BlockUtility.hashBlock(bytes)

    fun getHeight(): Int =
        BlockUtility.extractBlockHeightFromBlockHeader(bytes)

    fun getVersion(): Short =
        BlockUtility.extractVersionFromBlockHeader(bytes)

    fun getPreviousHash(): String =
        Utility.bytesToHex(BlockUtility.extractPreviousBlockHashFromBlockHeader(bytes))

    fun getSecondPreviousHash(): String =
        Utility.bytesToHex(BlockUtility.extractSecondPreviousBlockHashFromBlockHeader(bytes))

    fun getThirdPreviousHash(): String =
        Utility.bytesToHex(BlockUtility.extractThirdPreviousBlockHashFromBlockHeader(bytes))

    fun getMerkleRoot(): String =
        Utility.bytesToHex(BlockUtility.extractMerkleRootFromBlockHeader(bytes))

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
