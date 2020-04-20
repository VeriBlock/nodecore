// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.bitcoinj.utils.ContextPropagatingThreadFactory
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.core.VpmOperation
import org.veriblock.miners.pop.service.BitcoinService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ProcessManager(
    private val taskService: VpmTaskService,
    private val bitcoinService: BitcoinService
) {
    private val taskPool = Executors.newSingleThreadScheduledExecutor(
        ContextPropagatingThreadFactory("pop-tasks")
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
        // Launching from the bitcoin context in order to propagate it
        bitcoinService.contextCoroutineScope.launch {
            operation.job = coroutineScope.launch {
                taskService.runTasks(operation)
            }
        }
    }

    fun cancel(operation: VpmOperation) {
        if (operation.job == null) {
            error("Trying to cancel operation [${operation.id}] while it doesn't have a running job!")
        }
        operation.fail("Cancellation requested by the user")
    }

    private fun onReorg(operation: VpmOperation) {
        // Cancel that mining operation's job
        operation.stopJob()
        // Resubmit it
        submit(operation)
    }
}
