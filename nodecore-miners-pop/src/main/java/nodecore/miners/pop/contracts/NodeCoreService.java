// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import java.util.List;

public interface NodeCoreService {

    void shutdown() throws InterruptedException;

    boolean ping();

    NodeCoreReply<PoPMiningInstruction> getPop(Integer blockNumber);

    String submitPop(PoPMiningTransaction popMiningTransaction);

    List<PoPEndorsementInfo> getPoPEndorsementInfo();

    Integer getBitcoinBlockIndex(byte[] blockHeader);

    String getMinerAddress();

    VeriBlockHeader getLastBlock();

    Result unlockWallet(String passphrase);

    Result lockWallet();
}
