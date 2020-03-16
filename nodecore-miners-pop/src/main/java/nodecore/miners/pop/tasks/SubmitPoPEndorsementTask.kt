// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import io.grpc.StatusRuntimeException;
import nodecore.miners.pop.core.MiningOperation;
import nodecore.miners.pop.core.OperationState;
import nodecore.miners.pop.model.ApplicationExceptions;
import nodecore.miners.pop.model.PopMiningTransaction;
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
    protected TaskResult executeImpl(MiningOperation operation) {
        try {
            OperationState.Context state = (OperationState.Context) operation.getState();
            PopMiningTransaction popMiningTransaction =
                new PopMiningTransaction(state.getMiningInstruction(), state.getEndorsementTransaction().bitcoinSerialize(), state.getMerklePath(),
                    state.getBlockOfProof(), state.getBitcoinContextBlocks()
                );

            operation.setProofOfProofId(nodeCoreService.submitPop(popMiningTransaction));
            return TaskResult.succeed(operation, getNext());
        } catch (ApplicationExceptions.PoPSubmitRejected e) {
            logger.error("NodeCore rejected PoP submission");
            return failTask(operation, "NodeCore rejected PoP submission. Check NodeCore logs for detail.");
        } catch (StatusRuntimeException e) {
            logger.error("Failed to submit PoP transaction to NodeCore: {}", e.getStatus());
            return failTask(operation, "Unable to submit PoP transaction to NodeCore. Check that NodeCore RPC is available and resubmit operation.");
        }
    }
}
