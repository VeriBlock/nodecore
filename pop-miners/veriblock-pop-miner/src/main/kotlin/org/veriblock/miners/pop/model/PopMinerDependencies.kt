// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.model

import java.util.EnumSet

enum class PopMinerDependencies {
    SUFFICIENT_FUNDS,
    NODECORE_CONNECTED,
    SYNCHRONIZED_NODECORE,
    BLOCKCHAIN_DOWNLOADED,
    BITCOIN_SERVICE_READY;

    companion object {
        val SATISFIED = EnumSet.allOf(PopMinerDependencies::class.java)
    }
}
