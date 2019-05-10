// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import nodecore.miners.pop.storage.OperationStateData;

import java.util.Iterator;

public interface PoPRepository {
    Iterator<OperationStateData> getActiveOperations();

    OperationStateData getOperation(String id);

    long getUnconfirmedTransactionCount();

    void saveOperationState(OperationStateData stateData);
}
