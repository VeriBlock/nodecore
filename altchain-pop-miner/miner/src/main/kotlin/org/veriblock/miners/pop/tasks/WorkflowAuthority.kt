// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.tasks

import org.koin.core.KoinComponent
import org.koin.core.inject
import org.veriblock.core.utilities.createLogger
import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.Miner
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.core.error
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingMonitor
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.service.PluginService
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.core.utilities.createLogger
import java.util.concurrent.TimeUnit

private val logger = createLogger {}

class WorkflowAuthority(
    private val pluginFactory: PluginService,
    private val nodeCoreLiteKit: NodeCoreLiteKit,
    private val securityInheritingService: SecurityInheritingService
) : KoinComponent {

    val miner: Miner by inject()

    fun submit(operation: MiningOperation) {
        val chain = pluginFactory[operation.chainId]
        if (chain == null) {
            logger.warn { "Unable to load plugin ${operation.chainId} for operation $operation" }
            return
        }

        val monitor = securityInheritingService.getMonitor(operation.chainId)
        if (monitor == null) {
            logger.warn { "Unable to find monitor service ${operation.chainId} for operation $operation" }
            return
        }

        operation.stateChangedEvent.register(this) {
            onWorkflowStateChanged(it, operation, chain, monitor)
        }

        // Begin running
        operation.begin()

        Threading.TASK_POOL.submit {
            // Initial task
            executeTask(GetPublicationDataTask(miner, nodeCoreLiteKit, chain, monitor), operation)
        }
    }

    fun restore(operation: MiningOperation) {
        val chain = pluginFactory[operation.chainId]
        if (chain == null) {
            logger.warn { "Unable to load plugin ${operation.chainId} for operation $operation" }
            return
        }

        val monitor = securityInheritingService.getMonitor(operation.chainId)
        if (monitor == null) {
            logger.warn { "Unable to find monitor service ${operation.chainId} for operation $operation" }
            return
        }

        val changeHistory = operation.getChangeHistory()
        if (changeHistory.isNotEmpty()) {
            onWorkflowStateChanged(operation.state, operation, chain, monitor)
        }
    }

    private fun executeTask(task: BaseTask, operation: MiningOperation) {
        try {
            // Execute
            task.execute(operation)

            // Start next if any (recursive call)
            val next = task.next
            if (next != null) {
                executeTask(next, operation)
            }
        } catch (e: Exception) {
            logger.error(operation) { "Workflow failed (${e.message})" }
        }
    }

    private fun onWorkflowStateChanged(
        state: OperationState,
        operation: MiningOperation,
        chain: SecurityInheritingChain,
        monitor: SecurityInheritingMonitor
    ) {
        // We have to check by java class because each next state inherits from the previous, so for example all states
        // after Confirmed will evaluate `is OperationState.Confirmed` to true, and what we are looking at is the exact state
        when (state.javaClass) {
            OperationState.Confirmed::class.java -> Threading.TASK_POOL.submit {
                executeTask(DetermineBlockOfProofTask(miner, nodeCoreLiteKit, chain, monitor), operation)
            }
            OperationState.KeystoneOfProof::class.java -> Threading.TASK_POOL.submit {
                executeTask(RegisterVeriBlockPublicationPollingTask(miner, nodeCoreLiteKit, chain, monitor), operation)
            }
            OperationState.VeriBlockPublications::class.java -> Threading.TASK_POOL.submit {
                executeTask(SubmitProofOfProofTask(miner, nodeCoreLiteKit, chain, monitor), operation)
            }
            OperationState.SubmittedPopData::class.java -> Threading.TASK_POOL.submit {
                executeTask(AltEndorsementTransactionConfirmationTask(miner, nodeCoreLiteKit, chain, monitor), operation)
            }
            OperationState.AltEndorsementTransactionConfirmed::class.java -> Threading.TASK_POOL.submit {
                executeTask(AltEndorsedBlockConfirmationTask(miner, nodeCoreLiteKit, chain, monitor), operation)
            }
            OperationState.AltEndorsedBlockConfirmed::class.java -> Threading.TASK_POOL.submit {
                executeTask(PayoutDetectionTask(miner, nodeCoreLiteKit, chain, monitor), operation)
            }
            OperationState.Failed::class.java -> Threading.TASK_POOL.submit {
                executeTask(DeregisterVeriBlockPublicationPollingTask(miner, nodeCoreLiteKit, chain, monitor), operation)
            }
        }
    }
}
