// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import io.grpc.StatusRuntimeException;
import nodecore.miners.pop.core.PoPMiningOperationState;
import nodecore.miners.pop.model.ApplicationExceptions;
import nodecore.miners.pop.model.PoPMiningTransaction;
import nodecore.miners.pop.model.TaskResult;
import nodecore.miners.pop.services.BitcoinService;
import nodecore.miners.pop.services.NodeCoreService;

/**
 * Sixth and last task that will be executed in a mining operation
 */
public class SubmitPoPEndorsementTask extends BaseTask {
    @Override
    public BaseTask getNext() {
        return null;
    }

    public SubmitPoPEndorsementTask(NodeCoreService nodeCoreService, BitcoinService bitcoinService) {
        super(nodeCoreService, bitcoinService);
    }

    @Override
    protected TaskResult executeImpl(PoPMiningOperationState state) {
        try {
            PoPMiningTransaction popMiningTransaction = new PoPMiningTransaction(
                state.getMiningInstruction(),
                state.getTransactionBytes(),
                state.getMerklePath(),
                state.getBitcoinBlockHeaderOfProof(),
                state.getBitcoinContextBlocks()
            );

            state.onPoPTransactionSubmitted(nodeCoreService.submitPop(popMiningTransaction));
            return TaskResult.succeed(state, getNext());
        } catch (ApplicationExceptions.PoPSubmitRejected e) {
            logger.error("NodeCore rejected PoP submission");
            return failTask(state, "NodeCore rejected PoP submission. Check NodeCore logs for detail.");
        } catch (StatusRuntimeException e) {
            logger.error("Failed to submit PoP transaction to NodeCore: {}", e.getStatus());
            return failTask(state, "Unable to submit PoP transaction to NodeCore. Check that NodeCore RPC is available and resubmit operation.");
        }
    }
}
