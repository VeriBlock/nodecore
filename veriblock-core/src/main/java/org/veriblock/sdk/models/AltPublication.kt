// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.crypto.sha256HashOf

data class AltPublication(
    val transaction: VeriBlockTransaction,
    val merklePath: VeriBlockMerklePath,
    val blockOfProof: VeriBlockBlock
) {
    fun getId(): ByteArray =
        sha256HashOf(transaction.id.bytes + blockOfProof.hash.bytes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AltPublication) return false

        return transaction == other.transaction && merklePath == other.merklePath &&
            blockOfProof == other.blockOfProof
    }

    override fun hashCode(): Int {
        var result = transaction.hashCode()
        result = 31 * result + merklePath.hashCode()
        result = 31 * result + blockOfProof.hashCode()
        return result
    }
}
