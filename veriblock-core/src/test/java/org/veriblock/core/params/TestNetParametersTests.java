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

import static org.veriblock.core.params.NetworkParametersKt.defaultTestNetParameters;

public class TestNetParametersTests {
    @Test
    public void getInitialBitcoinBlockHeader() {
        byte[] header = defaultTestNetParameters.getBitcoinOriginBlock().raw;

        /* Compute the block hash by SHA256D on the header */
        Crypto crypto = new Crypto();
        byte[] result = crypto.SHA256D(header);
        String hash = Utility.bytesToHex(Utility.flip(result));

        Assert.assertEquals("000000007843C8E24359279C64BD4E4EF1B62D2A8C0557807C03124739566DD8", hash);
    }
}
