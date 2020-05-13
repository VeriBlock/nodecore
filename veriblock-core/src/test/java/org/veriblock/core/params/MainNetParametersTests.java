// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.params;

import org.junit.Assert;
import org.junit.Test;
import org.veriblock.core.crypto.Crypto;
import org.veriblock.core.utilities.Utility;

import static org.veriblock.core.params.NetworkParametersKt.defaultMainNetParameters;

public class MainNetParametersTests {
    @Test
    public void getInitialBitcoinBlockHeader() {
        byte[] header = defaultMainNetParameters.getBitcoinOriginBlock().raw;

        /* Compute the block hash by SHA256D on the header */
        Crypto crypto = new Crypto();
        byte[] result = crypto.SHA256D(header);
        String hash = Utility.bytesToHex(Utility.flip(result));

        Assert.assertTrue(hash.equalsIgnoreCase("00000000000000000022d067f3481a353ed7f2943219d5e006721c5498b5c09d"));
    }
}
