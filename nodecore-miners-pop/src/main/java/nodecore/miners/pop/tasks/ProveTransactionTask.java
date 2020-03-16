// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import nodecore.miners.pop.common.BitcoinMerklePath;
import nodecore.miners.pop.common.BitcoinMerkleTree;
import nodecore.miners.pop.common.MerkleProof;
import nodecore.miners.pop.core.MiningOperation;
import nodecore.miners.pop.core.OperationState;
import nodecore.miners.pop.model.TaskResult;
import nodecore.miners.pop.services.BitcoinService;
import nodecore.miners.pop.services.NodeCoreService;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.PartialMerkleTree;
import org.bitcoinj.core.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Fourth task that will be executed in a mining operation
 */
public class ProveTransactionTask extends BaseTask {
    @Override
    public BaseTask getNext() {
        return new BuildContextTask(nodeCoreService, bitcoinService);
    }

    public ProveTransactionTask(NodeCoreService nodeCoreService, BitcoinService bitcoinService) {
        super(nodeCoreService, bitcoinService);
    }

    @Override
    protected TaskResult executeImpl(MiningOperation operation) {
        try {
            OperationState.BlockOfProof state = (OperationState.BlockOfProof) operation.getState();
            Block block = state.getBlockOfProof();
            PartialMerkleTree partialMerkleTree = bitcoinService.getPartialMerkleTree(block.getHash());

            String failureReason = "";

            if (partialMerkleTree != null) {
                MerkleProof proof = MerkleProof.parse(partialMerkleTree);
                if (proof != null) {
                    String path = proof.getCompactPath(state.getEndorsementTransaction().getTxId());
                    logger.info("Merkle Proof Compact Path: {}", path);
                    logger.info("Merkle Root: {}", block.getMerkleRoot().toString());

                    try {
                        BitcoinMerklePath merklePath = new BitcoinMerklePath(path);
                        logger.info("Computed Merkle Root: {}", merklePath.getMerkleRoot());
                        if (merklePath.getMerkleRoot().equalsIgnoreCase(block.getMerkleRoot().toString())) {
                            operation.setMerklePath(merklePath.getCompactFormat());
                            return TaskResult.succeed(operation, getNext());
                        } else {
                            failureReason = "Block Merkle root does not match computed Merkle root";
                        }
                    } catch (Exception e) {
                        logger.error("Unable to validate Merkle path for transaction", e);
                        failureReason =
                            "Unable to prove transaction " + state.getEndorsementTransaction().getTxId() + " in block " + block.getHashAsString();
                    }
                } else {
                    failureReason = "Unable to construct Merkle proof for block " + block.getHashAsString();
                }
            } else {
                failureReason = "Unable to retrieve the Merkle tree from the block " + block.getHashAsString();
            }

            // Retrieving the Merkle path from the PartialMerkleTree failed, try creating manually from the whole block
            logger.info(
                "Unable to calculate the correct Merkle path for transaction " + state.getEndorsementTransaction().getTxId().toString() + " in block "
                    + state.getBlockOfProof().getHashAsString() + " from a FilteredBlock, trying a fully downloaded block!");

            Block fullBlock = bitcoinService.downloadBlock(state.getBlockOfProof().getHash());
            List<Transaction> allTransactions = fullBlock.getTransactions();

            List<String> txids = new ArrayList<>();
            for (int i = 0; i < allTransactions.size(); i++) {
                txids.add(allTransactions.get(i).getTxId().toString());
            }

            BitcoinMerkleTree bmt = new BitcoinMerkleTree(true, txids);
            BitcoinMerklePath merklePath = bmt.getPathFromTxID(state.getEndorsementTransaction().getTxId().toString());

            if (merklePath.getMerkleRoot().equalsIgnoreCase(state.getBlockOfProof().getMerkleRoot().toString())) {
                operation.setMerklePath(merklePath.getCompactFormat());
                return TaskResult.succeed(operation, getNext());
            } else {
                logger.error("Unable to calculate the correct Merkle path for transaction " + state.getEndorsementTransaction().getTxId().toString()
                    + " in block " + state.getBlockOfProof().getHashAsString() + " from a FilteredBlock or a fully downloaded block!");
                return failProcess(operation, failureReason);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return failProcess(operation, "Error proving transaction, see log for more detail.");
        }
    }
}
