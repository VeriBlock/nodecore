// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

public class TaskResult {
    private final boolean success;
    public boolean isSuccess() {
        return success;
    }

    private final PoPMiningOperationState state;
    public PoPMiningOperationState getState() {
        return state;
    }

    private final BaseTask next;
    public BaseTask getNext() {
        return next;
    }

    private TaskResult(PoPMiningOperationState state, boolean success, BaseTask next) {
        this.state = state;
        this.success = success;
        this.next = next;
    }

    public static TaskResult fail(PoPMiningOperationState state) {
        return new TaskResult(state, false, null);
    }

    public static TaskResult succeed(PoPMiningOperationState state, BaseTask next) {
        return new TaskResult(state, true, next);
    }
}