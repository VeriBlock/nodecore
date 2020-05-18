// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.util;

import org.junit.Assert;
import org.junit.Test;
import org.veriblock.core.utilities.Utility;

import java.math.BigInteger;
import java.util.BitSet;

public class UtilityTests {

    @Test
    public void toBytes_WhenLessThanSize() {
        BigInteger value = BigInteger.valueOf(999989182464L);

        byte[] result = Utility.toBytes(value, 12);
        byte[] expected = new byte[] {0,0,0,0,0,0,0,-24,-44,0,0,0} ;

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void verifySignature_WhenValid() {
        byte[] message = Utility.hexToBytes("0123456789ABCDEF");
        byte[] publicKey = Utility.hexToBytes("3056301006072A8648CE3D020106052B8104000A03420004CB427E41A0114874080A4B1E2AB7920E22CD2D188C87140DEFA447EE5FC44BB848E1C0DB5EF206DE2E7002F6C86952BE4823A4C08E65E4CDBEB904A8B95763AA");
        byte[] signature = Utility.hexToBytes("304402202F2B136EB22EDDACA3E9EA9C43A06478FF095108A19F433C358CE2C84461DE800220617E54A3FBC8B61B22D29772D58B27F47395915515F040E170BB50D951646C57");

        Assert.assertTrue(Utility.verifySignature(message, signature, publicKey));
    }

    @Test
    public void toInt() {
        byte[] hex = Utility.hexToBytes("24509D41F548C0");
        BitSet bitSet = BitSet.valueOf(org.veriblock.core.utilities.Utility.flip(hex));

        int offset = 6;
        Assert.assertEquals(35, Utility.toInt(bitSet.get(offset, offset + 8)));
        Assert.assertEquals(21, Utility.toInt(bitSet.get(offset + 8, offset + 14)));
        Assert.assertEquals(31, Utility.toInt(bitSet.get(offset + 14, offset + 22)));
        Assert.assertEquals(20, Utility.toInt(bitSet.get(offset + 22, offset + 28)));
        Assert.assertEquals(39, Utility.toInt(bitSet.get(offset + 28, offset + 36)));
        Assert.assertEquals(20, Utility.toInt(bitSet.get(offset + 36, offset + 42)));
        Assert.assertEquals(36, Utility.toInt(bitSet.get(offset + 42, bitSet.length())));
    }


    @Test
    public void hexWithOddLength() {
        //With Odd length
        String expectedValue = "1";
        byte[] hex = Utility.hexToBytes(expectedValue);
        String actualValue = Utility.bytesToHex(hex);

        Assert.assertTrue("01".equals(actualValue));
    }
}
