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
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.Miner
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.sdk.alt.plugin.PluginService

private val logger = createLogger {}

class WorkflowAuthority(
    private val pluginFactory: PluginService,
    private val nodeCoreLiteKit: NodeCoreLiteKit,
    private val securityInheritingService: SecurityInheritingService
) : KoinComponent {

    val miner: Miner by inject()

    private val coroutineScope = CoroutineScope(Threading.TASK_POOL.asCoroutineDispatcher())

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

        // Begin running
        operation.begin()

        coroutineScope.launch {
            // Initial task
            runTasks(miner, nodeCoreLiteKit, chain, monitor, operation)
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
            coroutineScope.launch {
                // Initial task
                runTasks(miner, nodeCoreLiteKit, chain, monitor, operation)
            }
        }
    }
}
