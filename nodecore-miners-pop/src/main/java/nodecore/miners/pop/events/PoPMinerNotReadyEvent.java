// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.events;

import nodecore.miners.pop.contracts.PoPMinerDependencies;

public class PoPMinerNotReadyEvent extends WarningMessageEvent {
    private final PoPMinerDependencies failedDependency;

    public PoPMinerDependencies getFailedDependency() {
        return failedDependency;
    }

    public PoPMinerNotReadyEvent(String message, PoPMinerDependencies failedDependency) {
        super(String.format("PoP Miner: NOT READY (%s)", message));
        this.failedDependency = failedDependency;
    }
}
