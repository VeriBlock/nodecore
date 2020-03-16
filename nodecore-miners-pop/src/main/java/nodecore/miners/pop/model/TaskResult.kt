// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.model

import nodecore.miners.pop.core.MiningOperation
import nodecore.miners.pop.tasks.BaseTask

class TaskResult private constructor(
    val state: MiningOperation,
    val isSuccess: Boolean,
    val next: BaseTask?
) {
    companion object {
        @JvmStatic
        fun fail(state: MiningOperation): TaskResult {
            return TaskResult(state, false, null)
        }

        @JvmStatic
        fun succeed(state: MiningOperation, next: BaseTask?): TaskResult {
            return TaskResult(state, true, next)
        }
    }
}
