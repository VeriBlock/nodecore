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
import org.veriblock.sdk.BlockStoreException
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.createLogger

private val logger = createLogger {}

class DetermineBlockOfProofTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = ProveTransactionTask(nodeCoreLiteKit, securityInheritingChain)

    override fun executeImpl(operation: MiningOperation) {
        val state = operation.state
        val transaction = (state as? OperationState.EndorsementTransaction)?.transaction
            ?: failTask("The operation has no transaction set!")

        val blockHash = transaction.transactionMeta.appearsInBestChainBlock
            ?: failTask("Unable to retrieve block of proof from transaction")

        try {
            val block = nodeCoreLiteKit.blockChain.get(blockHash)
                ?: failTask("Unable to retrieve VBK block $blockHash")
            operation.setBlockOfProof(block)
            logger.info(operation) { "Successfully added the VBK block of proof!" }
        } catch (e: BlockStoreException) {
            failTask("Error when retrieving VBK block $blockHash")
        }
    }
}
