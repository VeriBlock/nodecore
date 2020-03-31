// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model

import java.util.concurrent.atomic.AtomicReference

class BlockStore {
    private val chainHead = AtomicReference<VeriBlockHeader>()

    fun getChainHead(): VeriBlockHeader? {
        return chainHead.get()
    }

    fun setChainHead(blockBlockHeader: VeriBlockHeader) {
        chainHead.set(blockBlockHeader)
    }
}
