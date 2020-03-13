// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import nodecore.miners.pop.core.PoPMiningOperationState;
import nodecore.miners.pop.events.EventBus;
import nodecore.miners.pop.model.TaskResult;
import nodecore.miners.pop.services.BitcoinService;
import nodecore.miners.pop.services.NodeCoreService;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Sha256Hash;

import java.util.Map;

/**
 * Third task that will be executed in a mining operation
 */
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
        if (state.getTransaction() == null) {
            return TaskResult.fail(state);
        }

        /*
         * This mechanism is a quick hack to bypass instances where the FilteredBlockAvailableEvent is never
         * passed to the internal event bus, preventing the PoP transaction from progressing.
         */
        new Thread(() -> {
            try {
                Thread.sleep(30000L); // Delay 30 seconds
                PoPMiningOperationState.Action stateAction = state.getCurrentAction();
                if (stateAction == PoPMiningOperationState.Action.PROOF) {
                    // State still hasn't progressed past the PROOF action, meaning a FilteredBlockAvailableEvent
                    // Probably never occurred.
                    logger.info("Forcibly posting false filtered block available event...");
                    EventBus.INSTANCE.getFilteredBlockAvailableEvent().trigger(state);
                } else {
                    logger.info("Not forcibly posting filtered block available event; state action is not PROOF.");
                }
            } catch (Exception e) {
                logger.info("Exception occurred in the backup timer for providing alternate proof!");
            }
        }).start();

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
