// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.tasks;

import nodecore.miners.pop.common.BitcoinMerklePath;
import nodecore.miners.pop.common.BitcoinMerkleTree;
import nodecore.miners.pop.common.MerkleProof;
import nodecore.miners.pop.contracts.*;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.PartialMerkleTree;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

import java.util.ArrayList;
import java.util.List;

public class ProveTransactionTask extends BaseTask {
    @Override
    public BaseTask getNext() {
        return new BuildContextTask(nodeCoreService, bitcoinService);
    }

    public ProveTransactionTask(NodeCoreService nodeCoreService, BitcoinService bitcoinService) {
        super(nodeCoreService, bitcoinService);
    }

    @Override
    protected TaskResult executeImpl(PoPMiningOperationState state) {
        try {
            Block block = state.getBitcoinBlockHeaderOfProof();
            PartialMerkleTree partialMerkleTree = bitcoinService.getPartialMerkleTree(block.getHash());

            String failureReason = "";

            if (partialMerkleTree != null) {
                MerkleProof proof = MerkleProof.parse(partialMerkleTree);
                if (proof != null) {
                    String path = proof.getCompactPath(Sha256Hash.wrap(state.getSubmittedTransactionId()));
                    logger.info("Merkle Proof Compact Path: {}", path);
                    logger.info("Merkle Root: {}", block.getMerkleRoot().toString());

                    try {
                        BitcoinMerklePath merklePath = new BitcoinMerklePath(path);
                        logger.info("Computed Merkle Root: {}", merklePath.getMerkleRoot());
                        if (merklePath.getMerkleRoot().equalsIgnoreCase(block.getMerkleRoot().toString())) {
                            failureReason = "Block Merkle root does not match computed Merkle root";
                        } else {
                            state.onTransactionProven(merklePath.getCompactFormat());
                            return TaskResult.succeed(state, getNext());
                        }
                    } catch (Exception e) {
                        logger.error("Unable to validate Merkle path for transaction", e);
                        failureReason = "Unable to prove transaction " + state.getSubmittedTransactionId() + " in block " + block.getHashAsString();
                    }
                } else {
                    failureReason = "Unable to construct Merkle proof for block " + block.getHashAsString();
                }
            } else {
                failureReason = "Unable to retrieve the Merkle tree from the block " + block.getHashAsString();
            }

            // Retrieving the Merkle path from the PartialMerkleTree failed, try creating manually from the whole block
            logger.info("Unable to calculate the correct Merkle path for transaction " +
                    state.getTransaction().getHashAsString() + " in block " +
                    state.getBitcoinBlockHeaderOfProof().getHashAsString() +
                    " from a FilteredBlock, trying a fully downloaded block!");

            Block fullBlock = bitcoinService.downloadBlock(state.getBitcoinBlockHeaderOfProof().getHash());
            List<Transaction> allTransactions = fullBlock.getTransactions();

            List<String> txids = new ArrayList<>();
            for (int i = 0; i < allTransactions.size(); i++) {
                txids.add(allTransactions.get(i).getHashAsString());
            }

            BitcoinMerkleTree bmt = new BitcoinMerkleTree(true, txids);
            BitcoinMerklePath merklePath = bmt.getPathFromTxID(state.getSubmittedTransactionId());

            if (merklePath.getMerkleRoot().equalsIgnoreCase(state.getBitcoinBlockHeaderOfProof().getMerkleRoot().toString())) {
                state.onTransactionProven(merklePath.getCompactFormat());
                return TaskResult.succeed(state, getNext());
            } else {
                logger.error("Unable to calculate the correct Merkle path for transaction " +
                        state.getTransaction().getHashAsString() + " in block " +
                        state.getBitcoinBlockHeaderOfProof().getHashAsString() +
                        " from a FilteredBlock or a fully downloaded block!");
                return failProcess(state, failureReason);
            }


        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return failProcess(state, "Error proving transaction, see log for more detail.");
        }
    }
}
