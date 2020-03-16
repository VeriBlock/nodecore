// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.tasks

import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.model.TaskResult
import nodecore.miners.pop.model.TaskResult.Companion.fail
import nodecore.miners.pop.services.BitcoinService
import nodecore.miners.pop.services.NodeCoreService
import org.veriblock.core.utilities.createLogger

abstract class BaseTask protected constructor(
    protected val nodeCoreService: NodeCoreService,
    protected val bitcoinService: BitcoinService
) {
    abstract val next: BaseTask?

    protected abstract fun executeImpl(operation: MiningOperation): TaskResult

    fun execute(operation: MiningOperation): TaskResult {
        return try {
            executeImpl(operation)
        } catch (t: Throwable) {
            logger.error("Fatal error", t)
            failProcess(operation, "Fatal error")
        }
    }

    protected fun failProcess(operation: MiningOperation, reason: String): TaskResult {
        logger.warn("Operation {} failed for reason: {}", operation.id, reason)
        operation.fail(reason)
        return fail(operation)
    }

    protected fun failTask(operation: MiningOperation, reason: String): TaskResult {
        val output = String.format("[%s] %s", operation.id, reason)
        logger.error(output)
        return fail(operation)
    }

    companion object {
        @JvmStatic
        protected val logger = createLogger {}
    }
}
