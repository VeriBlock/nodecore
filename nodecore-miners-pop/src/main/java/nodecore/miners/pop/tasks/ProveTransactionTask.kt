// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.tasks

import nodecore.miners.pop.common.BitcoinMerklePath
import nodecore.miners.pop.common.BitcoinMerkleTree
import nodecore.miners.pop.common.MerkleProof
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.core.OperationState
import nodecore.miners.pop.model.TaskResult
import nodecore.miners.pop.model.TaskResult.Companion.succeed
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService
import java.util.ArrayList

/**
 * Fourth task that will be executed in a mining operation
 */
class ProveTransactionTask(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService
) : BaseTask(
    nodeCoreService, bitcoinService
) {
    override val next: BaseTask
        get() = BuildContextTask(nodeCoreService, bitcoinService)

    override fun executeImpl(operation: MiningOperation): TaskResult {
        return try {
            val state = operation.state as OperationState.BlockOfProof
            val block = state.blockOfProof
            val partialMerkleTree = bitcoinService.getPartialMerkleTree(block.hash)
            val failureReason = if (partialMerkleTree != null) {
                val proof = MerkleProof.parse(partialMerkleTree)
                if (proof != null) {
                    val path = proof.getCompactPath(state.endorsementTransaction.txId)
                    logger.info("Merkle Proof Compact Path: {}", path)
                    logger.info("Merkle Root: {}", block.merkleRoot.toString())
                    try {
                        val merklePath = BitcoinMerklePath(path)
                        logger.info("Computed Merkle Root: {}", merklePath.getMerkleRoot())
                        if (merklePath.getMerkleRoot().equals(block.merkleRoot.toString(), ignoreCase = true)) {
                            operation.setMerklePath(merklePath.getCompactFormat())
                            return succeed(operation, next)
                        } else {
                            "Block Merkle root does not match computed Merkle root"
                        }
                    } catch (e: Exception) {
                        logger.error("Unable to validate Merkle path for transaction", e)
                        "Unable to prove transaction " + state.endorsementTransaction.txId + " in block " + block.hashAsString
                    }
                } else {
                    "Unable to construct Merkle proof for block " + block.hashAsString
                }
            } else {
                "Unable to retrieve the Merkle tree from the block " + block.hashAsString
            }

            // Retrieving the Merkle path from the PartialMerkleTree failed, try creating manually from the whole block
            logger.info(
                "Unable to calculate the correct Merkle path for transaction " + state.endorsementTransaction.txId.toString() + " in block "
                    + state.blockOfProof.hashAsString + " from a FilteredBlock, trying a fully downloaded block!"
            )
            val fullBlock = bitcoinService.downloadBlock(state.blockOfProof.hash)
            val allTransactions = fullBlock!!.transactions
            val txids: MutableList<String> = ArrayList()
            for (i in allTransactions!!.indices) {
                txids.add(allTransactions[i].txId.toString())
            }
            val bmt = BitcoinMerkleTree(true, txids)
            val merklePath = bmt.getPathFromTxID(state.endorsementTransaction.txId.toString())
            if (merklePath!!.getMerkleRoot().equals(state.blockOfProof.merkleRoot.toString(), ignoreCase = true)) {
                operation.setMerklePath(merklePath.getCompactFormat())
                succeed(operation, next)
            } else {
                logger.error(
                    "Unable to calculate the correct Merkle path for transaction " + state.endorsementTransaction.txId.toString()
                        + " in block " + state.blockOfProof.hashAsString + " from a FilteredBlock or a fully downloaded block!"
                )
                failProcess(operation, failureReason)
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
            failProcess(operation, "Error proving transaction, see log for more detail.")
        }
    }
}
