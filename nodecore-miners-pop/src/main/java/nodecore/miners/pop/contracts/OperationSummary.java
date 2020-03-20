// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

public class OperationSummary {
    private final String operationId;

    public String getOperationId() {
        return operationId;
    }

    private final int endorsedBlockNumber;

    public int getEndorsedBlockNumber() {
        return endorsedBlockNumber;
    }

    private final String state;

    public String getState() {
        return state;
    }

    private final String action;

    public String getAction() {
        return action;
    }

    private final String message;

    public String getMessage() {
        return message;
    }

    public OperationSummary(String operationId, int endorsedBlockNumber, String state, String action, String message) {
        this.operationId = operationId;
        this.endorsedBlockNumber = endorsedBlockNumber;
        this.state = state;
        this.action = action;
        this.message = message;
    }
}
