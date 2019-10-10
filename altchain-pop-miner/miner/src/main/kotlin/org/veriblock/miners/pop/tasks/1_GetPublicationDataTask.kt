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
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.createLogger

private val logger = createLogger {}

class GetPublicationDataTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = CreateProofTransactionTask(nodeCoreLiteKit, securityInheritingChain)

    override fun executeImpl(operation: MiningOperation) {
        logger.info(operation) { "Getting the publication data..." }
        val state = operation.state
        if (state is OperationState.PublicationData) {
            logger.info(operation) { "Successfully retrieved the publication data!" }
            return
        }

        try {
            val publicationData = securityInheritingChain.getPublicationData(operation.blockHeight)
            operation.setPublicationDataWithContext(publicationData)
            logger.info(operation) { "Successfully added the publication data!" }
            return
        } catch (e: Exception) {
            failOperation(operation, "Failed to get PoP publication data from SI chain ${operation.chainId}: ${e.message}")
        }
    }
}
