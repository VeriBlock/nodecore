// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.rules;

import nodecore.miners.pop.contracts.VeriBlockHeader;

import java.util.List;

public class RuleContext {
    private VeriBlockHeader previousHead;

    public VeriBlockHeader getPreviousHead() {
        return previousHead;
    }

    public void setPreviousHead(VeriBlockHeader previousHead) {
        this.previousHead = previousHead;
    }

    private VeriBlockHeader latestBlock;

    public VeriBlockHeader getLatestBlock() {
        return latestBlock;
    }

    public void setLatestBlock(VeriBlockHeader latestBlock) {
        this.latestBlock = latestBlock;
    }

    private List<VeriBlockHeader> blocksRemoved;

    public List<VeriBlockHeader> getBlocksRemoved() {
        return blocksRemoved;
    }

    public void setBlocksRemoved(List<VeriBlockHeader> blocksRemoved) {
        this.blocksRemoved = blocksRemoved;
    }

    private List<VeriBlockHeader> blocksAdded;

    public List<VeriBlockHeader> getBlocksAdded() {
        return blocksAdded;
    }

    public void setBlocksAdded(List<VeriBlockHeader> blocksAdded) {
        this.blocksAdded = blocksAdded;
    }
}
