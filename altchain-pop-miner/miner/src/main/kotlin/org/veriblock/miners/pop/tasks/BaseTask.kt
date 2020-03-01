// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.tasks

import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.NodeCoreLiteKit
import org.veriblock.miners.pop.Miner
import org.veriblock.miners.pop.core.MiningOperation
import org.veriblock.miners.pop.core.debug
import org.veriblock.miners.pop.core.error
import org.veriblock.sdk.alt.SecurityInheritingChain

private val logger = createLogger {}

abstract class BaseTask protected constructor(
    protected val miner: Miner,
    protected val nodeCoreLiteKit: NodeCoreLiteKit,
    protected val securityInheritingChain: SecurityInheritingChain
) {
    abstract val next: BaseTask?

    protected abstract fun executeImpl(operation: MiningOperation)

    fun execute(operation: MiningOperation) {
        try {
            logger.debug(operation) { "Executing ${this.javaClass.simpleName}" }
            executeImpl(operation)
        } catch (e: ProcessException) {
            // Rethrow process exceptions
            throw e
        } catch (t: Throwable) {
            logger.error(operation, t) { "Error when executing task" }
        }
    }

    /**
     *  Throw an exception as the task failed. It is inline so that call stack is not polluted.
     */
    protected inline fun failTask(reason: String): Nothing {
        throw TaskException(reason)
    }

    /**
     *  Set the process as failed and throw an exception as the task failed. It is inline so that call stack is not polluted.
     */
    protected inline fun failOperation(operation: MiningOperation, reason: String): Nothing {
        operation.fail(reason)
        throw ProcessException(reason)
    }
}

class TaskException(message: String) : RuntimeException(message)
class ProcessException(message: String) : RuntimeException(message)
