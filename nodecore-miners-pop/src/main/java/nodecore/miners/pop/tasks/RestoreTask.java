// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

/**
 * Task that attempts to restore a mining operation that was left in progress
 */
/*public class RestoreTask extends BaseTask {
    private BaseTask next = null;

    @Override
    public BaseTask getNext() {
        return next;
    }

    public RestoreTask(NodeCoreService nodeCoreService, BitcoinService bitcoinService) {
        super(nodeCoreService, bitcoinService);
    }

    @Override
    protected TaskResult executeImpl(MiningOperation operation) {
        try {
            reconstitute(operation);

            if (operation.getTransaction() == null) {
                return failProcess(operation, "No Bitcoin transaction was found when restoring operation");
            }

            logger.info("[{}] Attempting to catch up with changes", operation.getOperationId());
            Map<Sha256Hash, Integer> blockHashes = operation.getTransaction().getAppearsInHashes();
            if (blockHashes == null || blockHashes.size() == 0) {
                // Still hasn't been seen, let the listeners wait
                return TaskResult.succeed(operation, null);
            }

            TransactionConfidence.ConfidenceType confidence = operation.getTransaction().getConfidence().getConfidenceType();
            logger.info("[{}] Transaction Confidence: {}", operation.getOperationId(), confidence.toString());
            // Transaction exists in a block in the best chain
            if (TransactionConfidence.ConfidenceType.BUILDING.equals(confidence)) {
                Block bestBlock = bitcoinService.getBestBlock(blockHashes.keySet());
                if (bestBlock == null) {
                    return failProcess(operation, "Unable to get block from store. Check log for details");
                }

                logger.info("[{}] Found block {}", operation.getOperationId(), bestBlock.getHashAsString());
                // New block of proof
                if (!bestBlock.equals(operation.getBitcoinBlockHeaderOfProof())) {
                    if (operation.getBitcoinBlockHeaderOfProof() != null) {
                        logger.info("[{}] Reorganize in Bitcoin blockchain", operation.getOperationId());
                        operation.onBitcoinReorganize();
                    }
                    logger.info("[{}] Setting as block header of proof", operation.getOperationId());
                    operation.onTransactionAppearedInBestChainBlock(bestBlock);
                }

                logger.info("[{}] Queuing up next task {}", operation.getOperationId(), operation.getCurrentActionAsString());
                switch (operation.getCurrentAction()) {
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

                return TaskResult.succeed(operation, getNext());
            } // Transaction exists in blocks not in best chain
            else if (TransactionConfidence.ConfidenceType.IN_CONFLICT.equals(confidence)) {
                if (operation.getBitcoinBlockHeaderOfProof() != null) {
                    logger.info("[{}] Reorganize in Bitcoin blockchain", operation.getOperationId());
                    operation.onBitcoinReorganize();
                }
                next = null;
                return TaskResult.succeed(operation, getNext());
            } // Transaction will not be confirmed without significant reorganization
            else if (TransactionConfidence.ConfidenceType.DEAD.equals(confidence)) {
                return failProcess(operation, "Transaction has been double spent and is no longer valid");
            }
        } catch (Exception e) {
            logger.error("Unable to restore operation {}", operation.getOperationId(), e);
        }

        return failProcess(operation, "Unable to restore operation");
    }

    private void reconstitute(PoPMiningOperationState state) {
        if (state.getTransactionBytes() != null) {
            logger.info("[{}] Rebuilding transaction", state.getOperationId());
            Transaction transaction = bitcoinService.makeTransaction(state.getTransactionBytes());
            state.setTransaction(transaction);
            state.registerListeners(transaction);
            logger.info("[{}] Rebuilt transaction {}", state.getOperationId(), transaction.getTxId().toString());
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
}*/
