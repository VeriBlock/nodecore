// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities;

import org.junit.Assert;
import org.junit.Test;
import org.veriblock.core.types.BitString;

public class BitStringReaderTests {
    @Test
    public void bitStringReaderTestSingleByte1() {
        // 01000000
        byte[] bytes = new byte[]{(byte)0x40};

        BitString bitString = new BitString(bytes);

        BitStringReader reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(1), new byte[]{(byte)0x00}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(7), new byte[]{(byte)0x40}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(2), new byte[]{(byte)0x01}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(6), new byte[]{(byte)0x00}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(3), new byte[]{(byte)0x02}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(5), new byte[]{(byte)0x00}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(4), new byte[]{(byte)0x04}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(4), new byte[]{(byte)0x00}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(5), new byte[]{(byte)0x08}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(3), new byte[]{(byte)0x00}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(6), new byte[]{(byte)0x10}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(2), new byte[]{(byte)0x00}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(7), new byte[]{(byte)0x20}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(1), new byte[]{(byte)0x00}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(8), new byte[]{(byte)0x40}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(3), new byte[]{(byte)0x02}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(2), new byte[]{(byte)0x00}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(3), new byte[]{(byte)0x00}));

        try {
            reader = new BitStringReader(bitString);
            reader.readBits(9);
            Assert.fail();
        } catch (Exception e) { }

        reader = new BitStringReader(bitString);
        Assert.assertEquals(reader.remaining(), 8);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 7);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 6);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 5);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 4);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 3);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 2);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 1);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 0);
    }

    @Test
    public void bitStringReaderTestSingleByte2() {
        // 10001010
        byte[] bytes = new byte[]{(byte)0x8A};

        BitString bitString = new BitString(bytes);

        BitStringReader reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(1), new byte[]{(byte)0x01}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(7), new byte[]{(byte)0x0A}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(2), new byte[]{(byte)0x02}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(6), new byte[]{(byte)0x0A}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(3), new byte[]{(byte)0x04}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(5), new byte[]{(byte)0x0A}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(4), new byte[]{(byte)0x08}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(4), new byte[]{(byte)0x0A}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(5), new byte[]{(byte)0x11}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(3), new byte[]{(byte)0x02}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(6), new byte[]{(byte)0x22}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(2), new byte[]{(byte)0x02}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(7), new byte[]{(byte)0x45}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(1), new byte[]{(byte)0x00}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(8), new byte[]{(byte)0x8A}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(1), new byte[]{(byte)0x01}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(4), new byte[]{(byte)0x01}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(2), new byte[]{(byte)0x01}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(1), new byte[]{(byte)0x00}));

        try {
            reader = new BitStringReader(bitString);
            reader.readBits(9);
            Assert.fail();
        } catch (Exception e) { }

        reader = new BitStringReader(bitString);
        Assert.assertEquals(reader.remaining(), 8);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 7);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 6);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 5);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 4);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 3);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 2);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 1);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 0);
    }

    @Test
    public void bitStringReaderTestDoubleByte1() {
        // 0011000101101101
        byte[] bytes = new byte[]{(byte)0x31, (byte)0x6D};

        BitString bitString = new BitString(bytes);

        BitStringReader reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(1), new byte[]{(byte)0x00}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(15), new byte[]{(byte)0x31, (byte)0x6D}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(2), new byte[]{(byte)0x00}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(14), new byte[]{(byte)0x31, (byte)0x6D}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(3), new byte[]{(byte)0x01}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(13), new byte[]{(byte)0x11, (byte)0x6D}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(4), new byte[]{(byte)0x03}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(12), new byte[]{(byte)0x01, (byte)0x6D}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(5), new byte[]{(byte)0x06}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(11), new byte[]{(byte)0x01, (byte)0x6D}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(6), new byte[]{(byte)0x0C}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(10), new byte[]{(byte)0x01, (byte)0x6D}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(7), new byte[]{(byte)0x18}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(9), new byte[]{(byte)0x01, (byte)0x6D}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(8), new byte[]{(byte)0x31}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(8), new byte[]{(byte)0x6D}));

        reader = new BitStringReader(bitString);
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(3), new byte[]{(byte)0x01}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(2), new byte[]{(byte)0x02}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(5), new byte[]{(byte)0x05}));
        Assert.assertTrue(Utility.byteArraysAreEqual(reader.readBits(6), new byte[]{(byte)0x2D}));

        try {
            reader = new BitStringReader(bitString);
            reader.readBits(5);
            reader.readBits(10);
            reader.readBits(2);
            Assert.fail();
        } catch (Exception e) { }

        reader = new BitStringReader(bitString);
        Assert.assertEquals(reader.remaining(), 16);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 15);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 14);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 13);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 12);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 11);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 10);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 9);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 8);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 7);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 6);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 5);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 4);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 3);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 2);
        Assert.assertEquals(reader.readBit(), 0x00);
        Assert.assertEquals(reader.remaining(), 1);
        Assert.assertEquals(reader.readBit(), 0x01);
        Assert.assertEquals(reader.remaining(), 0);
    }
}
