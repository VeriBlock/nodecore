// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
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

private val logger = createLogger {}

class WorkflowAuthority(
    private val pluginFactory: PluginService,
    private val nodeCoreLiteKit: NodeCoreLiteKit,
    private val securityInheritingService: SecurityInheritingService
) : KoinComponent {

    val miner: Miner by inject()

    val coroutineScope = CoroutineScope(Threading.TASK_POOL.asCoroutineDispatcher())

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

        coroutineScope.launch {
            // Initial task
            runTasks(miner, nodeCoreLiteKit, chain, operation, monitor)
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

    // TODO delete me
    private fun onWorkflowStateChanged(state: OperationState, operation: MiningOperation, chain: SecurityInheritingChain) {
    }
}
