// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.tasks

import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.bitcoinj.utils.ContextPropagatingThreadFactory
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.core.VpmOperation
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ProcessManager(
    private val taskService: VpmTaskService
) {
    private val taskPool = Executors.newFixedThreadPool(
        50,
        ThreadFactoryBuilder().apply {
            setNameFormat("pop-tasks-%d")
            setThreadFactory(ContextPropagatingThreadFactory("pop-tasks"))
        }.build()
    )
    private val coroutineScope = CoroutineScope(taskPool.asCoroutineDispatcher())

    init {
        EventBus.transactionSufferedReorgEvent.register(this, ::onReorg)
    }

    fun shutdown() {
        EventBus.transactionSufferedReorgEvent.unregister(this)

        // Shut down threading
        taskPool.shutdown()
        try {
            if (!taskPool.awaitTermination(10, TimeUnit.SECONDS)) {
                taskPool.shutdownNow()
            }
        } catch (ex: InterruptedException) {
            taskPool.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    fun submit(operation: VpmOperation) {
        if (operation.job != null) {
            error("Trying to submit operation [${operation.id}] while it already had a running job!")
        }
        operation.job = coroutineScope.launch {
            taskService.runTasks(operation)
        }
    }

    private fun onReorg(operation: VpmOperation) {
        // Cancel that mining operation's job
        operation.stopJob()
        // Resubmit it
        submit(operation)
    }
}
