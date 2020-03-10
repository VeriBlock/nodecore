// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.contracts

class TaskResult private constructor(
    val state: PoPMiningOperationState,
    val isSuccess: Boolean,
    val next: BaseTask?
) {
    companion object {
        @JvmStatic
        fun fail(state: PoPMiningOperationState): TaskResult {
            return TaskResult(state, false, null)
        }

        @JvmStatic
        fun succeed(state: PoPMiningOperationState, next: BaseTask?): TaskResult {
            return TaskResult(state, true, next)
        }
    }
}
