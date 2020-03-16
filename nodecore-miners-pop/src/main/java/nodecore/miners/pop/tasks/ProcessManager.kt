// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.tasks

import nodecore.miners.pop.Threading
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.events.EventBus
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService

class ProcessManager(
    private val nodeCoreService: NodeCoreService,
    private val bitcoinService: BitcoinService
) {
    init {
        EventBus.transactionConfirmedEvent.register(this, ::onTransactionConfirmed)
        EventBus.filteredBlockAvailableEvent.register(this, ::onFilteredBlockAvailable)
    }

    fun shutdown() {
        EventBus.transactionConfirmedEvent.unregister(this)
        EventBus.filteredBlockAvailableEvent.unregister(this)
    }

    fun submit(state: MiningOperation) {
        val task: BaseTask = GetPoPInstructionsTask(nodeCoreService, bitcoinService)
        Threading.TASK_POOL.submit { task.executeTask(state) }
    }

    private fun BaseTask.executeTask(state: MiningOperation) {
        val result = execute(state)
        if (result.isSuccess) {
            doNext(result.next, result.state)
        } else {
            handleFail(result.state)
        }
    }

    private fun handleFail(state: MiningOperation) {
        EventBus.popMiningOperationCompletedEvent.trigger(state.id)
    }

    private fun doNext(next: BaseTask?, state: MiningOperation) {
        next?.executeTask(state)
    }

    private fun onTransactionConfirmed(state: MiningOperation) {
        val task: BaseTask = DetermineBlockOfProofTask(nodeCoreService, bitcoinService)
        Threading.TASK_POOL.submit { task.executeTask(state) }
        return
    }

    private fun onFilteredBlockAvailable(state: MiningOperation) {
        val task: BaseTask = ProveTransactionTask(nodeCoreService, bitcoinService)
        Threading.TASK_POOL.submit { task.executeTask(state) }
        return
    }
}
