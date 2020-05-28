// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.sdk.models

class VeriBlockPublication(
    val transaction: VeriBlockPopTransaction,
    val merklePath: VeriBlockMerklePath,
    val containingBlock: VeriBlockBlock,
    val context: List<VeriBlockBlock> = emptyList()
) {
    fun getBlocks(): List<VeriBlockBlock> {
        return context + containingBlock
    }

    fun getFirstBlock(): VeriBlockBlock? {
        val blocks = getBlocks()
        return blocks.firstOrNull()
    }

    fun getFirstBitcoinBlock(): BitcoinBlock? {
        val blocks = transaction.getBlocks()
        return blocks.firstOrNull()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val obj = other as VeriBlockPublication
        return transaction == obj.transaction && merklePath == obj.merklePath && containingBlock == obj.containingBlock
    }

    override fun hashCode(): Int {
        var result = transaction.hashCode()
        result = 31 * result + merklePath.hashCode()
        result = 31 * result + containingBlock.hashCode()
        result = 31 * result + context.hashCode()
        return result
    }
}
