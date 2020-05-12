// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.blockchain;

import org.veriblock.sdk.models.BitcoinBlock;

import java.util.ArrayList;
import java.util.List;

public class BitcoinBlockchainBootstrapConfig {

    public List<BitcoinBlock> blocks;
    public int firstBlockHeight;

    public BitcoinBlockchainBootstrapConfig() {
        this.blocks = new ArrayList<BitcoinBlock>();
    }

    public BitcoinBlockchainBootstrapConfig(List<BitcoinBlock> blocks, int firstBlockHeight) {
        this.blocks = blocks;
        this.firstBlockHeight = firstBlockHeight;
    }

}
