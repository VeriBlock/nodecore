// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import java.util.concurrent.atomic.AtomicReference;

public class BlockStore {
    private AtomicReference<VeriBlockHeader> chainHead = new AtomicReference<>();
    public VeriBlockHeader getChainHead() {
        return chainHead.get();
    }
    public void setChainHead(VeriBlockHeader blockBlockHeader) {
        chainHead.set(blockBlockHeader);
    }

    public BlockStore() {

    }
}
