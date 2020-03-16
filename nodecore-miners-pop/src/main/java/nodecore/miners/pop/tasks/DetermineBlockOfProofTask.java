// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import nodecore.miners.pop.Threading;
import nodecore.miners.pop.core.MiningOperation;
import nodecore.miners.pop.core.OperationState;
import nodecore.miners.pop.events.EventBus;
import nodecore.miners.pop.model.TaskResult;
import nodecore.miners.pop.services.BitcoinService;
import nodecore.miners.pop.services.NodeCoreService;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.Sha256Hash;

import javax.annotation.Nullable;
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
    protected TaskResult executeImpl(MiningOperation operation) {
        if (!(operation.getState() instanceof OperationState.EndorsementTransaction)) {
            return TaskResult.fail(operation);
        }

        OperationState.EndorsementTransaction state = (OperationState.EndorsementTransaction) operation.getState();

        /*
         * This mechanism is a quick hack to bypass instances where the FilteredBlockAvailableEvent is never
         * passed to the internal event bus, preventing the PoP transaction from progressing.
         */
        new Thread(() -> {
            try {
                Thread.sleep(30000L); // Delay 30 seconds
                if (operation.getState() instanceof OperationState.BlockOfProof) {
                    // State still hasn't progressed past the PROOF action, meaning a FilteredBlockAvailableEvent
                    // Probably never occurred.
                    logger.info("Forcibly posting false filtered block available event...");
                    EventBus.INSTANCE.getFilteredBlockAvailableEvent().trigger(operation);
                } else {
                    logger.info("Not forcibly posting filtered block available event; state action is not PROOF.");
                }
            } catch (Exception e) {
                logger.info("Exception occurred in the backup timer for providing alternate proof!");
            }
        }).start();

        Map<Sha256Hash, Integer> blockAppearances = state.getEndorsementTransaction().getAppearsInHashes();
        if (blockAppearances != null) {
            Block bestBlock = bitcoinService.getBestBlock(blockAppearances.keySet());
            if (bestBlock != null) {
                operation.setBlockOfProof(bestBlock);
                Futures.addCallback(bitcoinService.getFilteredBlockFuture(bestBlock.getHash()), new FutureCallback<FilteredBlock>() {
                    @Override
                    public void onSuccess(@Nullable FilteredBlock result) {
                        EventBus.INSTANCE.getFilteredBlockAvailableEvent().trigger(operation);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        operation.fail(t.getMessage());
                    }
                }, Threading.TASK_POOL);

                return TaskResult.succeed(operation, getNext());
            }
        }

        return TaskResult.fail(operation);
    }
}
