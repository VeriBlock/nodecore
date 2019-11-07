// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import io.grpc.StatusRuntimeException;
import nodecore.miners.pop.contracts.BaseTask;
import nodecore.miners.pop.contracts.NodeCoreReply;
import nodecore.miners.pop.contracts.PoPMiningInstruction;
import nodecore.miners.pop.contracts.PoPMiningOperationState;
import nodecore.miners.pop.contracts.TaskResult;
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
    protected TaskResult executeImpl(PoPMiningOperationState state) {
        if (state.getMiningInstruction() != null) {
            return TaskResult.succeed(state, getNext());
        }

        state.begin();

        /* Get the PoPMiningInstruction, consisting of the 80 bytes of data that VeriBlock will pay the PoP miner
         * to publish to Bitcoin (includes 64-byte VB header and 16-byte miner ID) as well as the
         * PoP miner's address
         */
        try {
            NodeCoreReply<PoPMiningInstruction> popReply = nodeCoreService.getPop(state.getBlockNumber());
            if (popReply.getSuccess()) {
                state.onReceivedMiningInstructions(popReply.getResult());
                return TaskResult.succeed(state, getNext());
            } else {
                return failProcess(state, popReply.getResultMessage());
            }
        } catch (StatusRuntimeException e) {
            return failProcess(state, "Failed to get PoP publication data from NodeCore: " + e.getStatus());
        }
    }
}
