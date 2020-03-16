// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.model.dto;

import nodecore.miners.pop.core.OperationStateType;
import nodecore.miners.pop.core.OperationStatus;
import nodecore.miners.pop.model.PopMiningInstruction;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class PopMiningOperationStateDto implements Serializable {
    public String operationId;

    public OperationStatus status;

    public OperationStateType currentAction;

    public PopMiningInstruction miningInstruction;

    public byte[] transaction;

    public String submittedTransactionId;

    public byte[] bitcoinBlockHeaderOfProof;

    public List<byte[]> bitcoinContextBlocks;

    public String merklePath;

    public Set<byte[]> alternateBlocksOfProof;

    public String detail;

    public String popTransactionId;
}
