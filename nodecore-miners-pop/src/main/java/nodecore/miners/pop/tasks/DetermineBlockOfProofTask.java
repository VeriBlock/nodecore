// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import nodecore.miners.pop.contracts.*;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Sha256Hash;

import java.util.Map;

public class DetermineBlockOfProofTask extends BaseTask {
    @Override
    public BaseTask getNext() {
        return null;
    }

    public DetermineBlockOfProofTask(NodeCoreService nodeCoreService, BitcoinService bitcoinService) {
        super(nodeCoreService, bitcoinService);
    }

    @Override
    protected TaskResult executeImpl(PoPMiningOperationState state) {
        if (state.getTransaction() == null) return TaskResult.fail(state);

        Map<Sha256Hash, Integer> blockAppearances = state.getTransaction().getAppearsInHashes();
        if (blockAppearances != null) {
            Block bestBlock = bitcoinService.getBestBlock(blockAppearances.keySet());
            if (bestBlock != null) {
                state.onTransactionAppearedInBestChainBlock(bestBlock);
                state.registerFilteredBlockListener(bitcoinService.getFilteredBlockFuture(bestBlock.getHash()));

                return TaskResult.succeed(state, getNext());
            }
        }

        return TaskResult.fail(state);
    }
}
