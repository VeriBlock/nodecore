// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

import org.veriblock.core.crypto.Sha256Hash

data class AltPublication(
    val transaction: VeriBlockTransaction,
    val merklePath: VeriBlockMerklePath,
    val containingBlock: VeriBlockBlock,
    val context: List<VeriBlockBlock> = emptyList()
) {
    fun getId(): ByteArray =
        Sha256Hash.hash(transaction.id.bytes + containingBlock.hash.bytes)

    fun getBlocks(): List<VeriBlockBlock> =
        context + containingBlock

    fun getFirstBlock(): VeriBlockBlock =
        getBlocks().first()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AltPublication) return false

        return transaction == other.transaction && merklePath == other.merklePath &&
            containingBlock == other.containingBlock && context == other.context
    }

    override fun hashCode(): Int {
        var result = transaction.hashCode()
        result = 31 * result + merklePath.hashCode()
        result = 31 * result + containingBlock.hashCode()
        result = 31 * result + context.hashCode()
        return result
    }

    companion object {
        const val MAX_CONTEXT_COUNT = 15000
    }
}
