// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.tasks

import io.grpc.StatusRuntimeException
import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.core.OperationState
import nodecore.miners.pop.model.TaskResult
import nodecore.miners.pop.model.TaskResult.Companion.succeed
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService

/**
 * First task that will be executed in a mining operation
 */
class GetPoPInstructionsTask(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService
) : BaseTask(
    nodeCoreService, bitcoinService
) {
    override val next: BaseTask
        get() = CreateBitcoinTransactionTask(nodeCoreService, bitcoinService)

    override fun executeImpl(operation: MiningOperation): TaskResult {
        if (operation.state is OperationState.Instruction) {
            return succeed(operation, next)
        }

        /* Get the PoPMiningInstruction, consisting of the 80 bytes of data that VeriBlock will pay the PoP miner
         * to publish to Bitcoin (includes 64-byte VB header and 16-byte miner ID) as well as the
         * PoP miner's address
         */
        return try {
            val popReply = nodeCoreService.getPop(operation.blockHeight)
            if (popReply.success) {
                operation.setMiningInstruction(popReply.result!!)
                succeed(operation, next)
            } else {
                failProcess(operation, popReply.resultMessage!!)
            }
        } catch (e: StatusRuntimeException) {
            failProcess(operation, "Failed to get PoP publication data from NodeCore: " + e.status)
        }
    }
}
