// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.tasks

import org.veriblock.core.altchain.AltchainPoPEndorsement
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.extensions.toHex
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.info
import org.veriblock.miners.pop.minerConfig
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.services.SerializeDeserializeService

private val logger = createLogger {}

class CreateProofTransactionTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = null // We will wait for the transaction to be confirmed, which will trigger DetermineBlockOfProofTask

    override fun executeImpl(operation: MiningOperation) {
        val state = operation.state
        if (state is OperationState.EndorsementTransaction) {
            logger.info(operation) { "Successfully retrieved the VBK transaction: ${state.transaction.id}!" }
            return
        }

        if (state !is OperationState.PublicationData) {
            failTask("CreateProofTransactionTask called without publication data!")
        }

        // Something to fill in all the gaps
        logger.info(operation) { "Submitting endorsement VBK transaction..." }
        val transaction = try {
            val endorsementData = SerializeDeserializeService.serialize(state.publicationDataWithContext.publicationData)
            if (!AltchainPoPEndorsement.isValidEndorsement(endorsementData)) {
                failOperation(operation, "Invalid endorsement data: ${endorsementData.toHex()}")
            }
            nodeCoreLiteKit.network.submitEndorsement(
                endorsementData,
                minerConfig.feePerByte,
                minerConfig.maxFee
            )
        } catch (e: Exception) {
            failOperation(operation, "Could not create endorsement VBK transaction")
        }

        val walletTransaction = nodeCoreLiteKit.transactionMonitor.getTransaction(transaction.id)
        operation.setTransaction(walletTransaction)
        logger.info(operation) { "Successfully added the VBK transaction: ${walletTransaction.id}!" }
        logger.info(operation) { "Waiting for the transaction to be included in VeriBlock block..." }
    }
}
