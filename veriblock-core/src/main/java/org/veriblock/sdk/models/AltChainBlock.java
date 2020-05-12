// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

public class AltChainBlock extends BlockIndex implements Comparable<AltChainBlock>{
    private int timestamp;

    public AltChainBlock(String blockHash, long height, int timestamp)
    {
        super(height, blockHash);
        this.timestamp = timestamp;
    }

    public int getTimestamp() { return this.timestamp; }

    public boolean isKeystone(int keystoneInterval)
    {
        return height % keystoneInterval == 0;
    }

    @Override
    public int compareTo(AltChainBlock block) {
        return this.height > block.height ? 1: this.height < block.height ? -1 : 0;
    }
}
