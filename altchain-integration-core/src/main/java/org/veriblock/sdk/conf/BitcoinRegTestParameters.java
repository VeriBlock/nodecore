// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.conf;

import java.math.BigInteger;

public class BitcoinRegTestParameters implements BitcoinNetworkParameters {
    private static final BigInteger POW_LIMIT = new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

    @Override
    public BigInteger getPowLimit() {
        return POW_LIMIT;
    }
    
    @Override
    public int getPowTargetTimespan() {
        return 14 * 24 * 60 * 60;
    }

    @Override
    public int getPowTargetSpacing() {
        return 10 * 60;
    }

    @Override
    public boolean getAllowMinDifficultyBlocks() {
        return true;
    }

    @Override
    public boolean getPowNoRetargeting() {
        return true;
    }
}
