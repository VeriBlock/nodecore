// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import nodecore.miners.pop.common.Utility;
import nodecore.miners.pop.contracts.PoPMiningOperationState;
import nodecore.miners.pop.contracts.TaskResult;
import nodecore.miners.pop.services.BitcoinService;
import nodecore.miners.pop.services.NodeCoreService;
import org.bitcoinj.core.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fifth task that will be executed in a mining operation
 */
public class BuildContextTask extends BaseTask {

    @Override
    public BaseTask getNext() {
        return new SubmitPoPEndorsementTask(nodeCoreService, bitcoinService);
    }

    public BuildContextTask(NodeCoreService nodeCoreService, BitcoinService bitcoinService) {
        super(nodeCoreService, bitcoinService);
    }

    @Override
    protected TaskResult executeImpl(PoPMiningOperationState state) {
        try {
            boolean contextChainProvided = state.getMiningInstruction().endorsedBlockContextHeaders != null &&
                    state.getMiningInstruction().endorsedBlockContextHeaders.size() > 0;

            List<Block> context = new ArrayList<>();

            boolean found;
            Block current = state.getBitcoinBlockHeaderOfProof();
            do {
                Block previous = bitcoinService.getBlock(current.getPrevBlockHash());
                if (previous == null) {
                    throw new Exception(String.format("Could not retrieve block '%s'", current.getPrevBlockHash()));
                }

                if (contextChainProvided) {
                    found = state.getMiningInstruction().endorsedBlockContextHeaders.stream()
                            .anyMatch(header -> Arrays.equals(header, Utility.serializeBlock(previous)));
                    logger.info("{} block {} in endorsed block context headers", found ? "Found" : "Did not find", previous.getHashAsString());
                } else {
                    Integer blockIndex = nodeCoreService.getBitcoinBlockIndex(Utility.serializeBlock(previous));
                    found = blockIndex != null;
                    logger.info("{} block {} in search of current NodeCore view", found ? "Found" : "Did not find", previous.getHashAsString());
                }

                if (!found) {
                    context.add(0, previous);
                }

                current = previous;
            } while (!found);

            state.onBitcoinContextDetermined(context);
            return TaskResult.succeed(state, getNext());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return failTask(state, "Error building Bitcoin context, see log for more detail. Operation can be resubmitted.");
        }
    }
}
