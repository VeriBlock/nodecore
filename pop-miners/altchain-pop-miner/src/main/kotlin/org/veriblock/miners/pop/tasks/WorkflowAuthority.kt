// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
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
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.core.ApmOperation
import org.veriblock.miners.pop.service.MinerService

private val logger = createLogger {}

class WorkflowAuthority(
    private val taskService: ApmTaskService
) : KoinComponent {

    val miner: MinerService by inject()

    private val coroutineScope = CoroutineScope(Threading.TASK_POOL.asCoroutineDispatcher())

    fun submit(operation: ApmOperation) {
        if (operation.job != null) {
            error("Trying to submit operation [${operation.id}] while it already had a running job!")
        }
        operation.job = coroutineScope.launch {
            taskService.runTasks(operation)
        }
    }

    fun restore(operation: ApmOperation) {
        val changeHistory = operation.getChangeHistory()
        if (changeHistory.isNotEmpty()) {
            coroutineScope.launch {
                taskService.runTasks(operation)
            }
        }
    }
}
