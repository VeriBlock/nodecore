// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.models;

import java.util.List;

public class OperationDetailResponse {
    public String operationId;

    public String status;

    public String currentAction;

    public PoPDataResponse popData;

    public String transaction;

    public String submittedTransactionId;

    public String bitcoinBlockHeaderOfProof;

    public List<String> bitcoinContextBlocks;

    public String merklePath;

    public List<String> alternateBlocksOfProof;

    public String detail;

    public String popTransactionId;
}
