// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.events;

import nodecore.miners.pop.contracts.PoPMiningOperationState;

import java.util.List;

public class PoPMiningOperationStateChangedEvent {
    private PoPMiningOperationState state;

    public PoPMiningOperationState getState() {
        return state;
    }

    private List<String> messages;

    public List<String> getMessages() {
        return messages;
    }

    public PoPMiningOperationStateChangedEvent(PoPMiningOperationState state, List<String> messages) {
        this.state = state;
        this.messages = messages;
    }
}
