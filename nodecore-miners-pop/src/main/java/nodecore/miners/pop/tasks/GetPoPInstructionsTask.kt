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
import nodecore.miners.pop.model.NodeCoreReply;
import nodecore.miners.pop.model.PopMiningInstruction;
import nodecore.miners.pop.model.TaskResult;
import nodecore.miners.pop.services.BitcoinService;
import nodecore.miners.pop.services.NodeCoreService;

/**
 * First task that will be executed in a mining operation
 */
public class GetPoPInstructionsTask extends BaseTask {

    @Override
    public BaseTask getNext() {
        return new CreateBitcoinTransactionTask(nodeCoreService, bitcoinService);
    }

    public GetPoPInstructionsTask(NodeCoreService nodeCoreService, BitcoinService bitcoinService) {
        super(nodeCoreService, bitcoinService);
    }

    @Override
    protected TaskResult executeImpl(MiningOperation operation) {
        if (operation.getState() instanceof OperationState.Instruction) {
            return TaskResult.succeed(operation, getNext());
        }

        operation.begin();

        /* Get the PoPMiningInstruction, consisting of the 80 bytes of data that VeriBlock will pay the PoP miner
         * to publish to Bitcoin (includes 64-byte VB header and 16-byte miner ID) as well as the
         * PoP miner's address
         */
        try {
            NodeCoreReply<PopMiningInstruction> popReply = nodeCoreService.getPop(operation.getBlockHeight());
            if (popReply.getSuccess()) {
                operation.setMiningInstruction(popReply.getResult());
                return TaskResult.succeed(operation, getNext());
            } else {
                return failProcess(operation, popReply.getResultMessage());
            }
        } catch (StatusRuntimeException e) {
            return failProcess(operation, "Failed to get PoP publication data from NodeCore: " + e.getStatus());
        }
    }
}
