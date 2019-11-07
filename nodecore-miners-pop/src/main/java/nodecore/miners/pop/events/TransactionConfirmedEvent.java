// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.events;

import nodecore.miners.pop.contracts.PoPMiningOperationState;

public class TransactionConfirmedEvent {
    private final PoPMiningOperationState state;

    public PoPMiningOperationState getState() {
        return state;
    }

    public TransactionConfirmedEvent(PoPMiningOperationState state) {
        this.state = state;
    }
}
