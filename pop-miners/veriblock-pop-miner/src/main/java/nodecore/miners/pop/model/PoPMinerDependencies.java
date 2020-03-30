// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.model;

import java.util.EnumSet;

public enum PoPMinerDependencies {
    SUFFICIENT_FUNDS,
    NODECORE_CONNECTED,
    SYNCHRONIZED_NODECORE,
    BLOCKCHAIN_DOWNLOADED,
    BITCOIN_SERVICE_READY;

    public static final EnumSet<PoPMinerDependencies> SATISFIED = EnumSet.allOf(PoPMinerDependencies.class);
}
