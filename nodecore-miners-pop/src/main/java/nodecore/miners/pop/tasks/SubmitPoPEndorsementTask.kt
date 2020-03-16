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
import nodecore.miners.pop.model.ApplicationExceptions.PoPSubmitRejected
import nodecore.miners.pop.model.PopMiningTransaction
import nodecore.miners.pop.model.TaskResult
import nodecore.miners.pop.model.TaskResult.Companion.succeed
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService

/**
 * Sixth and last task that will be executed in a mining operation
 */
class SubmitPoPEndorsementTask(
    nodeCoreService: NodeCoreService,
    bitcoinService: BitcoinService
) : BaseTask(
    nodeCoreService, bitcoinService
) {
    override val next: BaseTask?
        get() = null

    override fun executeImpl(operation: MiningOperation): TaskResult {
        return try {
            val state = operation.state as OperationState.Context
            val popMiningTransaction = PopMiningTransaction(
                state.miningInstruction, state.endorsementTransaction.bitcoinSerialize(), state.merklePath,
                state.blockOfProof, state.bitcoinContextBlocks
            )
            operation.setProofOfProofId(nodeCoreService.submitPop(popMiningTransaction))
            succeed(operation, next)
        } catch (e: PoPSubmitRejected) {
            logger.error("NodeCore rejected PoP submission")
            failTask(operation, "NodeCore rejected PoP submission. Check NodeCore logs for detail.")
        } catch (e: StatusRuntimeException) {
            logger.error("Failed to submit PoP transaction to NodeCore: {}", e.status)
            failTask(operation, "Unable to submit PoP transaction to NodeCore. Check that NodeCore RPC is available and resubmit operation.")
        }
    }
}
