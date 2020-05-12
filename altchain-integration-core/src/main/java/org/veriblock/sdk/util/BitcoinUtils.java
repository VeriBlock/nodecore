// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.util;

import org.veriblock.sdk.models.Sha256Hash;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * <p>
 * Adapted from Utils class of BitcoinJ. Their license:
 * <p>
 * Copyright 2011 Google Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Provides various high-level utilities for interacting with data from/to Bitcoin.
 * <p>
 * Primarily, these utilities help in decoding difficulty from mantissa (basically scientific notation) format used in Bitcoin.
 *
 */
public class BitcoinUtils {
    /**
     * Returns the difficulty target as a 256 bit value that can be compared to a SHA-256 hash. Inside a block the
     * target is represented using a compact form.
     */
    public static BigInteger getDifficultyTargetAsInteger(int nBits) {
        return decodeCompactBits(nBits);
    }

    public static long readUint32BE(byte[] bytes, int offset) {
        return ((bytes[offset + 0] & 0xFFL) << 24) |
                ((bytes[offset + 1] & 0xFFL) << 16) |
                ((bytes[offset + 2] & 0xFFL) << 8) |
                ((bytes[offset + 3] & 0xFFL) << 0);
    }

    /**
     * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function. They consist of
     * a 4 byte big endian length field, followed by the stated number of bytes representing
     * the number in big endian format.
     */
    public static BigInteger decodeMPI(byte[] mpi) {
        int length = (int) readUint32BE(mpi, 0);
        byte[] buf = new byte[length];
        System.arraycopy(mpi, 4, buf, 0, length);
        return new BigInteger(buf);
    }

    /**
     * The representation of nBits uses another home-brew encoding, as a way to represent a large
     * hash value in only 32 bits.
     */
    public static BigInteger decodeCompactBits(long compact) {
        int size = ((int) (compact >> 24)) & 0xFF;
        byte[] bytes = new byte[4 + size];
        bytes[3] = (byte) size;
        if (size >= 1) bytes[4] = (byte) ((compact >> 16) & 0xFF);
        if (size >= 2) bytes[5] = (byte) ((compact >> 8) & 0xFF);
        if (size >= 3) bytes[6] = (byte) ((compact >> 0) & 0xFF);
        return decodeMPI(bytes);
    }

    public static long encodeCompactBits(BigInteger value) {
        int size = value.toByteArray().length;
        long result;
        if (size <= 3) {
            result = value.longValue() << 8 * (3 - size);
        } else {
            result = value.shiftRight(8 * (size - 3)).longValue();
        }

        if ((result & 8388608L) != 0L) {
            result >>= 8;
            ++size;
        }

        result |= (long)(size << 24);
        result |= value.signum() == -1 ? 8388608L : 0L;
        return result;
    }

    // use this method to get the PoW for the block higher than any possible block hash
    // this bits value is equal to very low difficulty for the block mining
    public static int bitcoinVeryHighPowEncodeToBits() {
        // prepare a very big hex encoded number
        char[] charArray = new char[Sha256Hash.BITCOIN_LENGTH * 2 + 2];
        Arrays.fill(charArray, 'F');
        String veryBigNumberStringHex = new String(charArray);

        long bits = BitcoinUtils.encodeCompactBits(new BigInteger(veryBigNumberStringHex, 16));
        return (int) bits;
    }
}
