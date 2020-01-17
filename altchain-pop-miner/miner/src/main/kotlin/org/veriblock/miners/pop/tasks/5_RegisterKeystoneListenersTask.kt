// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.tasks

import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.core.FullBlock
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.info
import org.veriblock.sdk.alt.SecurityInheritingChain

private val logger = createLogger {}

class RegisterKeystoneListenersTask(
    nodeCoreLiteKit: NodeCoreLiteKit,
    securityInheritingChain: SecurityInheritingChain
) : BaseTask(
    nodeCoreLiteKit, securityInheritingChain
) {
    override val next: BaseTask?
        get() = null // We are going to wait for the next Keystone, which will trigger RegisterVeriBlockPublicationPollingTask

    override fun executeImpl(operation: MiningOperation) {
        val state = operation.state
        if (state !is OperationState.BlockOfProof) {
            failTask("RegisterKeystoneListenersTask called without block of proof!")
        }
        // Register to nodecore's block events
        nodeCoreLiteKit.blockChain.newBestBlockEvent.register(operation) { newBlock ->
            val blockOfProof = state.blockOfProof
            // Wait for block of proof's keystone
            if (newBlock.height == blockOfProof.height / 20 * 20 + 20) {
                // Found!
                handleFoundKeystone(operation, newBlock)
            }
        }
        nodeCoreLiteKit.blockChain.blockChainReorganizedEvent.register(operation) {
            val blockOfProof = state.blockOfProof
            // Consider looking for keystone when blockchain reorganizes as well
            for (newBlock in it.newBlocks) {
                if (newBlock.height == blockOfProof.height / 20 * 20 + 20) {
                    handleFoundKeystone(operation, newBlock)
                    break
                }
            }
        }
        logger.info(operation) { "Successfully subscribed to VBK's new best block and blockchain reorg events!" }
        logger.info(operation) { "Waiting for the next VBK Keystone..." }
    }

    private fun handleFoundKeystone(state: MiningOperation, newBlock: FullBlock) {
        state.setKeystoneOfProof(newBlock)
        nodeCoreLiteKit.blockChain.newBestBlockEvent.remove(state)
        nodeCoreLiteKit.blockChain.blockChainReorganizedEvent.remove(state)
    }
}
