// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.tasks

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import nodecore.miners.pop.Threading
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.core.OperationState
import nodecore.miners.pop.events.EventBus.filteredBlockAvailableEvent
import nodecore.miners.pop.model.TaskResult
import nodecore.miners.pop.model.TaskResult.Companion.fail
import nodecore.miners.pop.model.TaskResult.Companion.succeed
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService
import org.bitcoinj.core.FilteredBlock

/**
 * Third task that will be executed in a mining operation
 */
class DetermineBlockOfProofTask(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService
) : BaseTask(
    nodeCoreService, bitcoinService
) {
    override val next: BaseTask?
        get() = null

    override fun executeImpl(operation: MiningOperation): TaskResult {
        if (operation.state !is OperationState.EndorsementTransaction) {
            return fail(operation)
        }
        val state = operation.state as OperationState.EndorsementTransaction

        /*
         * This mechanism is a quick hack to bypass instances where the FilteredBlockAvailableEvent is never
         * passed to the internal event bus, preventing the PoP transaction from progressing.
         */
        Thread(Runnable {
            try {
                Thread.sleep(30000L) // Delay 30 seconds
                if (operation.state is OperationState.BlockOfProof) {
                    // State still hasn't progressed past the PROOF action, meaning a FilteredBlockAvailableEvent
                    // Probably never occurred.
                    logger.info("Forcibly posting false filtered block available event...")
                    filteredBlockAvailableEvent.trigger(operation)
                } else {
                    logger.info("Not forcibly posting filtered block available event; state action is not PROOF.")
                }
            } catch (e: Exception) {
                logger.info("Exception occurred in the backup timer for providing alternate proof!")
            }
        }).start()
        val blockAppearances = state.endorsementTransaction.appearsInHashes
        if (blockAppearances != null) {
            val bestBlock = bitcoinService.getBestBlock(blockAppearances.keys)
            if (bestBlock != null) {
                operation.setBlockOfProof(bestBlock)
                Futures.addCallback(
                    bitcoinService.getFilteredBlockFuture(bestBlock.hash),
                    object : FutureCallback<FilteredBlock?> {
                        override fun onSuccess(result: FilteredBlock?) {
                            filteredBlockAvailableEvent.trigger(operation)
                        }

                        override fun onFailure(t: Throwable) {
                            operation.fail(t.message!!)
                        }
                    },
                    Threading.TASK_POOL
                )
                return succeed(operation, next)
            }
        }
        return fail(operation)
    }
}
