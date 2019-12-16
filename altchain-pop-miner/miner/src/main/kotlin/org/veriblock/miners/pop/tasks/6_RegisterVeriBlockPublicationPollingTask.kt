// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.tasks

import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.PublicationSubscription
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.info
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.core.utilities.createLogger
import org.veriblock.sdk.util.Utils

private val logger = createLogger {}

class RegisterVeriBlockPublicationPollingTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = null // We will be waiting for this operation's veriblock publication, which will trigger the SubmitProofOfProofTask

    override fun executeImpl(operation: MiningOperation) {
        val state = operation.state
        if (state !is OperationState.KeystoneOfProof) {
            failTask("RegisterVeriBlockPublicationPollingTask called without keystone of proof!")
        }
        val subscription = PublicationSubscription(
            state.keystoneOfProof.hash.toString(),
            Utils.encodeHex(state.publicationDataWithContext.context[0]),
            Utils.encodeHex(state.publicationDataWithContext.btcContext[0])
        ) { publications ->
            operation.setVeriBlockPublications(publications)
        }

        nodeCoreLiteKit.network.addVeriBlockPublicationSubscription(operation.id, subscription)
        logger.info(operation) {
            """Successfully added publication subscription!
                |   - Keystone Hash: ${subscription.keystoneHash}
                |   - VBK Context Hash: ${subscription.contextHash}
                |   - BTC Context Hash: ${subscription.btcContextHash}""".trimMargin()
        }
        logger.info(operation) { "Waiting for this operation's veriblock publication..." }
    }
}
