// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.tasks

import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.info
import org.veriblock.sdk.Sha256Hash
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.core.utilities.createLogger

private val logger = createLogger {}

class ProveTransactionTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = RegisterKeystoneListenersTask(nodeCoreLiteKit, securityInheritingChain)

    override fun executeImpl(operation: MiningOperation) {
        val state = operation.state
        if (state !is OperationState.BlockOfProof) {
            failTask("ProveTransactionTask called without VBK block of proof!")
        }
        val walletTransaction = state.transaction

        logger.info(operation) { "Getting the merkle path for the transaction: ${walletTransaction.id}..." }
        val merklePath = walletTransaction.merklePath
            ?: failOperation(operation, "No merkle path found for ${walletTransaction.id}")
        logger.info(operation) { "Successfully retrieved the merkle path for the transaction: ${walletTransaction.id}!" }

        val vbkMerkleRoot = merklePath.merkleRoot.trim(Sha256Hash.VERIBLOCK_MERKLE_ROOT_LENGTH)
        val verified = vbkMerkleRoot == state.blockOfProof.merkleRoot
        if (!verified) {
            failOperation(operation, "Unable to verify merkle path! VBK Transaction's merkle root: $vbkMerkleRoot; Block of proof's merkle root: ${state.blockOfProof.merkleRoot}")
        }

        operation.setMerklePath(merklePath)
        logger.info(operation) { "Successfully added the verified merkle path!" }
    }
}
