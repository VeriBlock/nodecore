// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.bitcoinj;

import org.veriblock.core.crypto.Crypto;
import org.veriblock.core.utilities.BlockUtility;
import org.veriblock.core.utilities.TransactionEmbeddedDataUtility;
import org.veriblock.core.utilities.Utility;

import java.math.BigInteger;

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
public class BitcoinUtilities {
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
        long result;
        int size = value.toByteArray().length;
        if (size <= 3)
            result = value.longValue() << 8 * (3 - size);
        else
            result = value.shiftRight(8 * (size - 3)).longValue();
        // The 0x00800000 bit denotes the sign.
        // Thus, if it is already set, divide the mantissa by 256 and increase the exponent.
        if ((result & 0x00800000L) != 0) {
            result >>= 8;
            size++;
        }
        result |= size << 24;
        result |= value.signum() == -1 ? 0x00800000 : 0;
        return result;
    }

    public static byte[] extractPoPData(byte[] bitcoinTransaction) {
        for (int i = 0; i <= bitcoinTransaction.length - 80; i++) {
            try {
                byte[] potentialPoPPublication = new byte[80];
                System.arraycopy(bitcoinTransaction, i, potentialPoPPublication, 0, potentialPoPPublication.length);
                byte[] potentialHeader = new byte[64];
                System.arraycopy(potentialPoPPublication, 0, potentialHeader, 0, potentialHeader.length);

                if (BlockUtility.isPlausibleBlockHeader(potentialHeader)) {
                    return potentialPoPPublication;
                }
            } catch (Exception ignored) {}
        }

        try {
            return TransactionEmbeddedDataUtility.extractEmbeddedSplitData(bitcoinTransaction);
        } catch (Exception e) {
            return null;
        }
    }
    /**
     * <p>
     * The pseudo-constructor which does the heavy-lifting behind constructing an SPV Bitcoin block.
     *
     * @param version              The version integer [4 bytes]
     * @param previousBlockHashHex The hex representation of the previous Bitcoin block [32 bytes]
     * @param merkleRootHashHex    The hex representation of this block's Merkle root [32 bytes]
     * @param timestamp            The (UNIX-epoch-style) timestamp [4 bytes]
     * @param bits                 The nBits representation of the target, similar to scientific notation ('inverse' of Bitcoin difficulty) [4 bytes]
     * @param nonce                The winning nonce which, in the context of the rest of the header data (all above fields) creates a valid PoW solution [4 bytes]
     */
    public static byte[] constructBitcoinBlockHeader(
            int version,
            String previousBlockHashHex,
            String merkleRootHashHex,
            int timestamp,
            int bits,
            int nonce) {
        /* Space for the 80-byte header */
        byte[] header = new byte[4 + 32 + 32 + 4 + 4 + 4];

        /* Copy the header data, in order, into the 80-byte header */
        System.arraycopy(Utility.flip(Utility.intToByteArray(version)), 0, header, 0, 4);
        System.arraycopy(Utility.flip(Utility.hexToBytes(previousBlockHashHex)), 0, header, 4, 32);
        System.arraycopy(Utility.flip(Utility.hexToBytes(merkleRootHashHex)), 0, header, 36, 32);
        System.arraycopy(Utility.flip(Utility.intToByteArray(timestamp)), 0, header, 68, 4);
        System.arraycopy(Utility.flip(Utility.intToByteArray(bits)), 0, header, 72, 4);
        System.arraycopy(Utility.flip(Utility.longToByteArray(nonce)), 0, header, 76, 4);

        return header;
    }
}
