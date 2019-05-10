// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class PreservedPoPMiningOperationState implements Serializable {
    public String operationId;

    public PoPMiningOperationStatus status;

    public PoPMiningOperationState.Action currentAction;

    public PoPMiningInstruction miningInstruction;

    public byte[] transaction;

    public String submittedTransactionId;

    public byte[] bitcoinBlockHeaderOfProof;

    public List<byte[]> bitcoinContextBlocks;

    public String merklePath;

    public Set<byte[]> alternateBlocksOfProof;

    public String detail;

    public String popTransactionId;
}
