// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.tasks

import nodecore.miners.pop.common.Utility
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.core.OperationState.Proven
import nodecore.miners.pop.model.TaskResult
import nodecore.miners.pop.model.TaskResult.Companion.succeed
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService
import org.bitcoinj.core.Block
import java.util.ArrayList
import java.util.Arrays

/**
 * Fifth task that will be executed in a mining operation
 */
class BuildContextTask(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService
) : BaseTask(
    nodeCoreService, bitcoinService
) {
    override val next: BaseTask
        get() = SubmitPoPEndorsementTask(nodeCoreService, bitcoinService)

    override fun executeImpl(operation: MiningOperation): TaskResult {
        return try {
            val state = operation.state as Proven
            val contextChainProvided = (state.miningInstruction.endorsedBlockContextHeaders != null
                && state.miningInstruction.endorsedBlockContextHeaders.size > 0)
            val context = ArrayList<Block>()
            var found: Boolean
            var current = state.blockOfProof
            do {
                val previous = bitcoinService.getBlock(current.prevBlockHash)
                    ?: throw Exception(String.format("Could not retrieve block '%s'", current.prevBlockHash))
                if (contextChainProvided) {
                    found = state.miningInstruction.endorsedBlockContextHeaders.stream()
                        .anyMatch { header: ByteArray? ->
                            Arrays.equals(
                                header, Utility.serializeBlock(previous)
                            )
                        }
                    logger.info(
                        "{} block {} in endorsed block context headers", if (found) "Found" else "Did not find", previous.hashAsString
                    )
                } else {
                    val blockIndex = nodeCoreService.getBitcoinBlockIndex(Utility.serializeBlock(previous))
                    found = blockIndex != null
                    logger.info(
                        "{} block {} in search of current NodeCore view", if (found) "Found" else "Did not find", previous.hashAsString
                    )
                }
                if (!found) {
                    context.add(0, previous)
                }
                current = previous
            } while (!found)
            operation.setContext(context)
            succeed(operation, next)
        } catch (e: Exception) {
            logger.error(e.message, e)
            failTask(operation, "Error building Bitcoin context, see log for more detail. Operation can be resubmitted.")
        }
    }
}
