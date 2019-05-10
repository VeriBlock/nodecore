// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.models;

public class OperationSummaryResponse {
    public String operationId;
    public int endorsedBlockNumber;
    public String state;
    public String action;
    public String message;
}
