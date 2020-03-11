// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import nodecore.miners.pop.contracts.PoPMiningOperationState;
import nodecore.miners.pop.contracts.TaskResult;
import nodecore.miners.pop.services.BitcoinService;
import nodecore.miners.pop.services.NodeCoreService;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Task that attempts to restore a mining operation that was left in progress
 */
public class RestoreTask extends BaseTask {
    private BaseTask next = null;

    @Override
    public BaseTask getNext() {
        return next;
    }

    public RestoreTask(NodeCoreService nodeCoreService, BitcoinService bitcoinService) {
        super(nodeCoreService, bitcoinService);
    }

    @Override
    protected TaskResult executeImpl(PoPMiningOperationState state) {
        try {
            reconstitute(state);

            if (state.getTransaction() == null) {
                return failProcess(state, "No Bitcoin transaction was found when restoring operation");
            }

            logger.info("[{}] Attempting to catch up with changes", state.getOperationId());
            Map<Sha256Hash, Integer> blockHashes = state.getTransaction().getAppearsInHashes();
            if (blockHashes == null || blockHashes.size() == 0) {
                // Still hasn't been seen, let the listeners wait
                return TaskResult.succeed(state, null);
            }

            TransactionConfidence.ConfidenceType confidence = state.getTransaction().getConfidence().getConfidenceType();
            logger.info("[{}] Transaction Confidence: {}", state.getOperationId(), confidence.toString());
            // Transaction exists in a block in the best chain
            if (TransactionConfidence.ConfidenceType.BUILDING.equals(confidence)) {
                Block bestBlock = bitcoinService.getBestBlock(blockHashes.keySet());
                if (bestBlock == null) {
                    return failProcess(state, "Unable to get block from store. Check log for details");
                }

                logger.info("[{}] Found block {}", state.getOperationId(), bestBlock.getHashAsString());
                // New block of proof
                if (!bestBlock.equals(state.getBitcoinBlockHeaderOfProof())) {
                    if (state.getBitcoinBlockHeaderOfProof() != null) {
                        logger.info("[{}] Reorganize in Bitcoin blockchain", state.getOperationId());
                        state.onBitcoinReorganize();
                    }
                    logger.info("[{}] Setting as block header of proof", state.getOperationId());
                    state.onTransactionAppearedInBestChainBlock(bestBlock);
                }

                logger.info("[{}] Queuing up next task {}", state.getOperationId(), state.getCurrentActionAsString());
                switch (state.getCurrentAction()) {
                    case PROOF:
                        next = new ProveTransactionTask(nodeCoreService, bitcoinService);
                        break;
                    case CONTEXT:
                        next = new BuildContextTask(nodeCoreService, bitcoinService);
                        break;
                    case SUBMIT:
                        next = new SubmitPoPEndorsementTask(nodeCoreService, bitcoinService);
                        break;
                    case CONFIRM:
                        next = null;
                        break;
                }

                return TaskResult.succeed(state, getNext());
            } // Transaction exists in blocks not in best chain
            else if (TransactionConfidence.ConfidenceType.IN_CONFLICT.equals(confidence)) {
                if (state.getBitcoinBlockHeaderOfProof() != null) {
                    logger.info("[{}] Reorganize in Bitcoin blockchain", state.getOperationId());
                    state.onBitcoinReorganize();
                }
                next = null;
                return TaskResult.succeed(state, getNext());
            } // Transaction will not be confirmed without significant reorganization
            else if (TransactionConfidence.ConfidenceType.DEAD.equals(confidence)) {
                return failProcess(state, "Transaction has been double spent and is no longer valid");
            }
        } catch (Exception e) {
            logger.error("Unable to restore operation {}", state.getOperationId(), e);
        }

        return failProcess(state, "Unable to restore operation");
    }

    private void reconstitute(PoPMiningOperationState state) {
        if (state.getTransactionBytes() != null) {
            logger.info("[{}] Rebuilding transaction", state.getOperationId());
            Transaction transaction = bitcoinService.makeTransaction(state.getTransactionBytes());
            state.setTransaction(transaction);
            state.registerListeners(transaction);
            logger.info("[{}] Rebuilt transaction {}", state.getOperationId(), transaction.getHashAsString());
        }

        if (state.getBitcoinBlockHeaderOfProofBytes() != null) {
            logger.info("[{}] Rebuilding block of proof", state.getOperationId());
            Block block = bitcoinService.makeBlock(state.getBitcoinBlockHeaderOfProofBytes());
            state.setBitcoinBlockHeaderOfProof(block);
            logger.info("[{}] Reattached block of proof {}", state.getOperationId(), block.getHashAsString());
        }

        if (state.getBitcoinContextBlocksBytes() != null) {
            logger.info("[{}] Rebuilding context blocks", state.getOperationId());
            Collection<Block> blocks = bitcoinService.makeBlocks(state.getBitcoinContextBlocksBytes());
            state.setBitcoinContextBlocks(new ArrayList<>(blocks));
        }
    }
}
