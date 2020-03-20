// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities;

import org.veriblock.core.types.BitString;

public class BitStringReader {
    private final BitString bitString;
    private int marker = -1;
    public BitStringReader(BitString bitString) {
        this.bitString = bitString;
    }

    public int remaining() {
        return bitString.length() - marker - 1;
    }

    public boolean hasMore() {
        return remaining() > 0;
    }

    public byte readBit() {
        if (marker >= bitString.length() - 1) {
            throw new IllegalArgumentException("Already at end of bit string!");
        }

        marker++;
        return bitString.getBit(marker);
    }

    public byte[] readBits(int numBits) {
        if (numBits < 0) {
            throw new IllegalArgumentException("Unable to read a negative number of bits!");
        }

        if (numBits == 0) {
            return new byte[]{};
        }

        byte[] bits = new byte[numBits + (numBits % 8 == 0 ? 0 : (8 - (numBits % 8)))];
        for (int i = (numBits % 8 == 0 ? 0 : (8 - (numBits % 8))); i < bits.length; i++) {
            bits[i] = readBit();
        }

        byte[] bytes = new byte[bits.length / 8];

        int byteNum = bytes.length - 1;
        for (int i = bits.length - 1; i >= 0; i-=8) {
            int readByte =  (bits[i]) |
                    (bits[i - 1] << 1) |
                    (bits[i - 2] << 2) |
                    (bits[i - 3] << 3) |
                    (bits[i - 4] << 4) |
                    (bits[i - 5] << 5) |
                    (bits[i - 6] << 6) |
                    (bits[i - 7] << 7);

            bytes[byteNum] = (byte)readByte;
            byteNum--;
        }

        return bytes;
    }

    public static void main(String[] args) {
        byte[] bitStringBytes = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        BitString bitString = new BitString(bitStringBytes);
        BitStringReader reader = new BitStringReader(bitString);
        System.out.println(Utility.bytesToHex(reader.readBits(1)));
        System.out.println(Utility.bytesToHex(reader.readBits(2)));
        System.out.println(Utility.bytesToHex(reader.readBits(3)));
        System.out.println(Utility.bytesToHex(reader.readBits(4)));
        System.out.println(Utility.bytesToHex(reader.readBits(5)));
        System.out.println(Utility.bytesToHex(reader.readBits(6)));
        System.out.println(Utility.bytesToHex(reader.readBits(7)));
        System.out.println(Utility.bytesToHex(reader.readBits(8)));
        System.out.println(Utility.bytesToHex(reader.readBits(4)));
    }
}
