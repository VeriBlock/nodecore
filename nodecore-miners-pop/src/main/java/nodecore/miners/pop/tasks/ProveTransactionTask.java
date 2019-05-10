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

            if (partialMerkleTree == null) {
                return failProcess(state, "Unable to retrieve the merkle tree from the block " + block.getHashAsString());
            }

            MerkleProof proof = MerkleProof.parse(partialMerkleTree);
            if (proof == null) {
                return failProcess(state, "Unable to construct merkle proof for block " + block.getHashAsString());
            }

            String path = proof.getCompactPath(Sha256Hash.wrap(state.getSubmittedTransactionId()));
            logger.info("Merkle Proof Compact Path: {}", path);
            logger.info("Merkle Root: {}", block.getMerkleRoot().toString());

            try {
                BitcoinMerklePath merklePath = new BitcoinMerklePath(path);
                logger.info("Computed Merkle Root: {}", merklePath.getMerkleRoot());
                if (!merklePath.getMerkleRoot().equalsIgnoreCase(block.getMerkleRoot().toString())) {
                    logger.info("Block merkle root does not match computed merkle root");
                    return failProcess(state, "Unable to prove transaction " + state.getSubmittedTransactionId() + " in block " + block.getHashAsString());
                } else {
                    state.onTransactionProven(merklePath.getCompactFormat());

                    return TaskResult.succeed(state, getNext());
                }
            } catch (Exception e) {
                logger.error("Unable to validate merkle path for transaction", e);
                return failProcess(state, "Unable to prove transaction " + state.getSubmittedTransactionId() + " in block " + block.getHashAsString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return failProcess(state, "Error proving transaction, see log for more detail.");
        }
    }
}
