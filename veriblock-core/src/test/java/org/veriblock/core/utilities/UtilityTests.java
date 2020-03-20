// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities;

import org.junit.Assert;
import org.junit.Test;

public class UtilityTests {

    @Test
    public void flipWhenEvenNumberLength() {
        byte[] test = new byte[]{(byte)0xFF, (byte)0xAA, (byte)0x55, (byte)0x00};

        byte[] result = Utility.flip(test);

        Assert.assertEquals(test[3], result[0]);
        Assert.assertEquals(test[2], result[1]);
        Assert.assertEquals(test[1], result[2]);
        Assert.assertEquals(test[0], result[3]);
    }

    @Test
    public void flipWhenOddNumberLength() {
        byte[] test = new byte[]{(byte)0xFF, (byte)0x88, (byte)0x00};

        byte[] result = Utility.flip(test);

        Assert.assertEquals(test[2], result[0]);
        Assert.assertEquals(test[1], result[1]);
        Assert.assertEquals(test[0], result[2]);
    }

    @Test
    public void flipHex() {
        String version = "02000000";

        String flipped = Utility.flipHex(version);

        Assert.assertEquals("00000002", flipped);
    }
}
