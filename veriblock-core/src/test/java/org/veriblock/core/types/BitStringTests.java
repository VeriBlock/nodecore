// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.types;

import org.junit.Assert;
import org.junit.Test;

public class BitStringTests {

    @Test
    public void bitStringTestSingleByte1() {
        byte[] bytes = new byte[]{(byte)0x40};

        BitString bitString = new BitString(bytes);

        Assert.assertEquals(bitString.toString(), "01000000");

        Assert.assertEquals(bitString.getBit(0), (byte)0x00);
        Assert.assertEquals(bitString.getBit(1), (byte)0x01);
        Assert.assertEquals(bitString.getBit(2), (byte)0x00);
        Assert.assertEquals(bitString.getBit(3), (byte)0x00);
        Assert.assertEquals(bitString.getBit(4), (byte)0x00);
        Assert.assertEquals(bitString.getBit(5), (byte)0x00);
        Assert.assertEquals(bitString.getBit(6), (byte)0x00);
        Assert.assertEquals(bitString.getBit(7), (byte)0x00);

        Assert.assertEquals(bitString.getBoolean(0), false);
        Assert.assertEquals(bitString.getBoolean(1), true);
        Assert.assertEquals(bitString.getBoolean(2), false);
        Assert.assertEquals(bitString.getBoolean(3), false);
        Assert.assertEquals(bitString.getBoolean(4), false);
        Assert.assertEquals(bitString.getBoolean(5), false);
        Assert.assertEquals(bitString.getBoolean(6), false);
        Assert.assertEquals(bitString.getBoolean(7), false);
    }

    @Test
    public void bitStringTestSingleByte2() {
        byte[] bytes = new byte[]{(byte)0xF7};

        BitString bitString = new BitString(bytes);

        Assert.assertEquals(bitString.toString(), "11110111");

        Assert.assertEquals(bitString.getBit(0), (byte)0x01);
        Assert.assertEquals(bitString.getBit(1), (byte)0x01);
        Assert.assertEquals(bitString.getBit(2), (byte)0x01);
        Assert.assertEquals(bitString.getBit(3), (byte)0x01);
        Assert.assertEquals(bitString.getBit(4), (byte)0x00);
        Assert.assertEquals(bitString.getBit(5), (byte)0x01);
        Assert.assertEquals(bitString.getBit(6), (byte)0x01);
        Assert.assertEquals(bitString.getBit(7), (byte)0x01);

        Assert.assertEquals(bitString.getBoolean(0), true);
        Assert.assertEquals(bitString.getBoolean(1), true);
        Assert.assertEquals(bitString.getBoolean(2), true);
        Assert.assertEquals(bitString.getBoolean(3), true);
        Assert.assertEquals(bitString.getBoolean(4), false);
        Assert.assertEquals(bitString.getBoolean(5), true);
        Assert.assertEquals(bitString.getBoolean(6), true);
        Assert.assertEquals(bitString.getBoolean(7), true);
    }

    @Test
    public void bitStringTestSingleByte3() {
        byte[] bytes = new byte[]{(byte)0xA3};

        BitString bitString = new BitString(bytes);

        Assert.assertEquals(bitString.toString(), "10100011");

        Assert.assertEquals(bitString.getBit(0), (byte)0x01);
        Assert.assertEquals(bitString.getBit(1), (byte)0x00);
        Assert.assertEquals(bitString.getBit(2), (byte)0x01);
        Assert.assertEquals(bitString.getBit(3), (byte)0x00);
        Assert.assertEquals(bitString.getBit(4), (byte)0x00);
        Assert.assertEquals(bitString.getBit(5), (byte)0x00);
        Assert.assertEquals(bitString.getBit(6), (byte)0x01);
        Assert.assertEquals(bitString.getBit(7), (byte)0x01);

        Assert.assertEquals(bitString.getBoolean(0), true);
        Assert.assertEquals(bitString.getBoolean(1), false);
        Assert.assertEquals(bitString.getBoolean(2), true);
        Assert.assertEquals(bitString.getBoolean(3), false);
        Assert.assertEquals(bitString.getBoolean(4), false);
        Assert.assertEquals(bitString.getBoolean(5), false);
        Assert.assertEquals(bitString.getBoolean(6), true);
        Assert.assertEquals(bitString.getBoolean(7), true);
    }


    @Test
    public void bitStringTestDoubleByte1() {
        byte[] bytes = new byte[]{(byte)0x40, (byte)0xF7};

        BitString bitString = new BitString(bytes);

        Assert.assertEquals(bitString.toString(), "0100000011110111");
        Assert.assertEquals(bitString.getBit(0), (byte)0x00);
        Assert.assertEquals(bitString.getBit(1), (byte)0x01);
        Assert.assertEquals(bitString.getBit(2), (byte)0x00);
        Assert.assertEquals(bitString.getBit(3), (byte)0x00);
        Assert.assertEquals(bitString.getBit(4), (byte)0x00);
        Assert.assertEquals(bitString.getBit(5), (byte)0x00);
        Assert.assertEquals(bitString.getBit(6), (byte)0x00);
        Assert.assertEquals(bitString.getBit(7), (byte)0x00);
        Assert.assertEquals(bitString.getBit(8), (byte)0x01);
        Assert.assertEquals(bitString.getBit(9), (byte)0x01);
        Assert.assertEquals(bitString.getBit(10), (byte)0x01);
        Assert.assertEquals(bitString.getBit(11), (byte)0x01);
        Assert.assertEquals(bitString.getBit(12), (byte)0x00);
        Assert.assertEquals(bitString.getBit(13), (byte)0x01);
        Assert.assertEquals(bitString.getBit(14), (byte)0x01);
        Assert.assertEquals(bitString.getBit(15), (byte)0x01);

        Assert.assertEquals(bitString.getBoolean(0), false);
        Assert.assertEquals(bitString.getBoolean(1), true);
        Assert.assertEquals(bitString.getBoolean(2), false);
        Assert.assertEquals(bitString.getBoolean(3), false);
        Assert.assertEquals(bitString.getBoolean(4), false);
        Assert.assertEquals(bitString.getBoolean(5), false);
        Assert.assertEquals(bitString.getBoolean(6), false);
        Assert.assertEquals(bitString.getBoolean(7), false);
        Assert.assertEquals(bitString.getBoolean(8), true);
        Assert.assertEquals(bitString.getBoolean(9), true);
        Assert.assertEquals(bitString.getBoolean(10), true);
        Assert.assertEquals(bitString.getBoolean(11), true);
        Assert.assertEquals(bitString.getBoolean(12), false);
        Assert.assertEquals(bitString.getBoolean(13), true);
        Assert.assertEquals(bitString.getBoolean(14), true);
        Assert.assertEquals(bitString.getBoolean(15), true);
    }

    @Test
    public void bitStringTestDoubleByte2() {
        byte[] bytes = new byte[]{(byte)0xF7, (byte)0xC9};

        BitString bitString = new BitString(bytes);

        Assert.assertEquals(bitString.toString(), "1111011111001001");
        Assert.assertEquals(bitString.getBit(0), (byte)0x01);
        Assert.assertEquals(bitString.getBit(1), (byte)0x01);
        Assert.assertEquals(bitString.getBit(2), (byte)0x01);
        Assert.assertEquals(bitString.getBit(3), (byte)0x01);
        Assert.assertEquals(bitString.getBit(4), (byte)0x00);
        Assert.assertEquals(bitString.getBit(5), (byte)0x01);
        Assert.assertEquals(bitString.getBit(6), (byte)0x01);
        Assert.assertEquals(bitString.getBit(7), (byte)0x01);
        Assert.assertEquals(bitString.getBit(8), (byte)0x01);
        Assert.assertEquals(bitString.getBit(9), (byte)0x01);
        Assert.assertEquals(bitString.getBit(10), (byte)0x00);
        Assert.assertEquals(bitString.getBit(11), (byte)0x00);
        Assert.assertEquals(bitString.getBit(12), (byte)0x01);
        Assert.assertEquals(bitString.getBit(13), (byte)0x00);
        Assert.assertEquals(bitString.getBit(14), (byte)0x00);
        Assert.assertEquals(bitString.getBit(15), (byte)0x01);

        Assert.assertEquals(bitString.getBoolean(0), true);
        Assert.assertEquals(bitString.getBoolean(1), true);
        Assert.assertEquals(bitString.getBoolean(2), true);
        Assert.assertEquals(bitString.getBoolean(3), true);
        Assert.assertEquals(bitString.getBoolean(4), false);
        Assert.assertEquals(bitString.getBoolean(5), true);
        Assert.assertEquals(bitString.getBoolean(6), true);
        Assert.assertEquals(bitString.getBoolean(7), true);
        Assert.assertEquals(bitString.getBoolean(8), true);
        Assert.assertEquals(bitString.getBoolean(9), true);
        Assert.assertEquals(bitString.getBoolean(10), false);
        Assert.assertEquals(bitString.getBoolean(11), false);
        Assert.assertEquals(bitString.getBoolean(12), true);
        Assert.assertEquals(bitString.getBoolean(13), false);
        Assert.assertEquals(bitString.getBoolean(14), false);
        Assert.assertEquals(bitString.getBoolean(15), true);
    }

    @Test
    public void bitStringTestManyBytes() {
        byte[] bytes = new byte[512];
        String expectedBitString = "";
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)i;
            String convertedBitString = Integer.toBinaryString(bytes[i] & 0xFF);
            while (convertedBitString.length() < 8) {
                convertedBitString = '0' + convertedBitString;
            }
            expectedBitString += convertedBitString;
        }

        BitString bitString = new BitString(bytes);

        Assert.assertEquals(bitString.toString(), expectedBitString);

        for (int i = 0; i < bitString.length(); i++) {
            Assert.assertEquals(bitString.getBit(i), (expectedBitString.charAt(i) == '0' ? (byte)0x00 : (byte)0x01));
            Assert.assertEquals(bitString.getBoolean(i), (expectedBitString.charAt(i) != '0'));
        }
    }
}
