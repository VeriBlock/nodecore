// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import nodecore.miners.pop.Threading
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.events.EventBus
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService

class ProcessManager(
    private val nodeCoreService: NodeCoreService,
    private val bitcoinService: BitcoinService
) {
    private val coroutineScope = CoroutineScope(Threading.TASK_POOL.asCoroutineDispatcher())

    init {
        EventBus.transactionSufferedReorgEvent.register(this, ::onReorg)
    }

    fun shutdown() {
        EventBus.transactionSufferedReorgEvent.unregister(this)
    }

    fun submit(operation: MiningOperation) {
        coroutineScope.launch {
            runTasks(nodeCoreService, bitcoinService, operation)
        }
    }

    private fun onReorg(operation: MiningOperation) {
        // Cancel that mining operation's job
        operation.stopJob()
        // Resubmit it
        submit(operation)
    }
}
