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
import org.veriblock.miners.pop.core.info
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.core.utilities.createLogger

private val logger = createLogger {}

class DeregisterVeriBlockPublicationPollingTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = null

    override fun executeImpl(operation: MiningOperation) {
        nodeCoreLiteKit.network.removeVeriBlockPublicationSubscription(operation.id)
        logger.info(operation) { "Successfully removed the publication subscription!" }
    }
}
