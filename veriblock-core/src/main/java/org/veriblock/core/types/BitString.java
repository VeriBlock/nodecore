// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.types;

public class BitString {
    private final String bits;
    public BitString(byte[] source) {
        StringBuilder converted = new StringBuilder();
        for (int i = 0; i < source.length; i++) {
            byte byteToConvert = source[i];
            for (int j = 0x80; j != 0; j >>>= 1) {
                converted.append((((byteToConvert & j) != 0) ? "1" : "0"));
            }
        }

        bits = converted.toString();
    }

    public BitString(String bits) {
        for (int i = 0; i < bits.length(); i++) {
            if (bits.charAt(i) != '0' && bits.charAt(i) != '1') {
                throw new IllegalArgumentException("A BitString cannot be constructed with a string (" + bits + ") which isn't binary!");
            }
        }

        this.bits = bits;
    }

    public static void main(String[] args) {
        System.out.println(new BitString(new byte[]{(byte)131}).bits);
    }

    public boolean getBoolean(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("getBoolean cannot be called with a negative index!");
        } else if (index >= bits.length()) {
            throw new IllegalArgumentException("getBoolean cannot be called with an index (" + index + ") larger than the length of the BitString (" + bits.length() + ")!");
        }

        return bits.charAt(index) == '1';
    }

    public byte getBit(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("getBit cannot be called with a negative index!");
        } else if (index >= bits.length()) {
            throw new IllegalArgumentException("getBit cannot be called with an index (" + index + ") larger than the length of the BitString (" + bits.length() + ")!");
        }

        return bits.charAt(index) == '1' ? (byte)1 : (byte)0;
    }

    public int length() {
        return bits.length();
    }

    public String toString() {
        return bits;
    }
}
