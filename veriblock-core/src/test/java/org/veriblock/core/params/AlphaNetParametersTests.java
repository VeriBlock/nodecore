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

public class AlphaNetParametersTests {
    @Test
    public void getInitialBitcoinBlockHeader() {
        NetworkParameters params = new AlphaNetParameters();
        byte[] header = params.getInitialBitcoinBlockHeader();

        /* Compute the block hash by SHA256D on the header */
        Crypto crypto = new Crypto();
        byte[] result = crypto.SHA256D(header);
        String hash = Utility.bytesToHex(Utility.flip(result));

        Assert.assertTrue(hash.equalsIgnoreCase("000000000000000246200f09b513e517a3bd8c591a3b692d9852ddf1ee0f8b3a"));
    }
}
