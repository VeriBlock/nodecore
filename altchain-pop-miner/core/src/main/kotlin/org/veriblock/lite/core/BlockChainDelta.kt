// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.core

import org.veriblock.sdk.VeriBlockBlock
import java.util.*

class BlockChainDelta(
        private val removed: List<VeriBlockBlock>,
        private val added: MutableList<VeriBlockBlock>
) {
    private var continuationHash: String? = null

    fun getRemoved(): List<VeriBlockBlock> {
        return Collections.unmodifiableList(removed)
    }

    fun getAdded(): List<VeriBlockBlock> {
        return Collections.unmodifiableList(added)
    }

    fun concat(other: BlockChainDelta) {
        this.added.addAll(other.added)
        this.continuationHash = other.continuationHash
    }
}
