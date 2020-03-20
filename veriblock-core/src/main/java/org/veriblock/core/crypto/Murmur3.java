// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

/*
 * MurmurHash3 was written by Austin Appleby, and is placed in the public
 * domain. The author hereby disclaims copyright to this source code.
 */

/*
 * Source:
 * http://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp
 * (Modified to adapt to Guava coding conventions and to use the HashFunction interface)
 */

package org.veriblock.core.crypto;

import org.veriblock.core.utilities.Utility;

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * See MurmurHash3_x86_32 in <a
 * href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">the C++
 * implementation</a>.
 *
 * @author Austin Appleby
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 */
public final class Murmur3 implements Serializable {
    private static final int CHUNK_SIZE = 4;

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;

    public static int hashInt(int seed, int input) {
        int k1 = mixK1(input);
        int h1 = mixH1(seed, k1);

        return fmix(h1, 4);
    }

    public static int hashLong(int seed, long input) {
        int low = (int) input;
        int high = (int) (input >>> 32);

        int k1 = mixK1(low);
        int h1 = mixH1(seed, k1);

        k1 = mixK1(high);
        h1 = mixH1(h1, k1);

        return fmix(h1, 8);
    }

    public static int hashUnencodedChars(int seed, CharSequence input) {
        int h1 = seed;

        // step through the CharSequence 2 chars at a time
        for (int i = 1; i < input.length(); i += 2) {
            int k1 = input.charAt(i - 1) | (input.charAt(i) << 16);
            k1 = mixK1(k1);
            h1 = mixH1(h1, k1);
        }

        // deal with any remaining characters
        if ((input.length() & 1) == 1) {
            int k1 = input.charAt(input.length() - 1);
            k1 = mixK1(k1);
            h1 ^= k1;
        }

        return fmix(h1, 2 * input.length());
    }

    public static int hashString(int seed, CharSequence input, Charset charset) {
        if (Charset.forName("UTF-8").equals(charset)) {
            int utf16Length = input.length();
            int h1 = seed;
            int i = 0;
            int len = 0;

            // This loop optimizes for pure ASCII.
            while (i + 4 <= utf16Length) {
                char c0 = input.charAt(i);
                char c1 = input.charAt(i + 1);
                char c2 = input.charAt(i + 2);
                char c3 = input.charAt(i + 3);
                if (c0 < 0x80 && c1 < 0x80 && c2 < 0x80 && c3 < 0x80) {
                    int k1 = c0 | (c1 << 8) | (c2 << 16) | (c3 << 24);
                    k1 = mixK1(k1);
                    h1 = mixH1(h1, k1);
                    i += 4;
                    len += 4;
                } else {
                    break;
                }
            }

            long buffer = 0;
            int shift = 0;
            for (; i < utf16Length; i++) {
                char c = input.charAt(i);
                if (c < 0x80) {
                    buffer |= (long) c << shift;
                    shift += 8;
                    len++;
                } else if (c < 0x800) {
                    buffer |= charToTwoUtf8Bytes(c) << shift;
                    shift += 16;
                    len += 2;
                } else if (c < Character.MIN_SURROGATE || c > Character.MAX_SURROGATE) {
                    buffer |= charToThreeUtf8Bytes(c) << shift;
                    shift += 24;
                    len += 3;
                } else {
                    int codePoint = Character.codePointAt(input, i);
                    if (codePoint == c) {
                        // not a valid code point; let the JDK handle invalid Unicode
                        return hashBytes(seed, input.toString().getBytes(charset));
                    }
                    i++;
                    buffer |= codePointToFourUtf8Bytes(codePoint) << shift;
                    len += 4;
                }

                if (shift >= 32) {
                    int k1 = mixK1((int) buffer);
                    h1 = mixH1(h1, k1);
                    buffer = buffer >>> 32;
                    shift -= 32;
                }
            }

            int k1 = mixK1((int) buffer);
            h1 ^= k1;
            return fmix(h1, len);
        } else {
            return hashBytes(seed, input.toString().getBytes(charset));
        }
    }

    public static int hashBytes(int seed, byte[] input) {
        return hashBytes(seed, input, 0, input.length);
    }

    public static int hashBytes(int seed, byte[] input, int off, int len) {
        int h1 = seed;
        int i;
        for (i = 0; i + CHUNK_SIZE <= len; i += CHUNK_SIZE) {
            int k1 = mixK1(getIntLittleEndian(input, off + i));
            h1 = mixH1(h1, k1);
        }

        int k1 = 0;
        for (int shift = 0; i < len; i++, shift += 8) {
            k1 ^= toInt(input[off + i]) << shift;
        }
        h1 ^= mixK1(k1);
        return fmix(h1, len);
    }

    private static int getIntLittleEndian(byte[] input, int offset) {
        return Utility.bytesToInt(input[offset + 3], input[offset + 2], input[offset + 1], input[offset]);
    }

    private static int mixK1(int k1) {
        k1 *= C1;
        k1 = Integer.rotateLeft(k1, 15);
        k1 *= C2;
        return k1;
    }

    private static int mixH1(int h1, int k1) {
        h1 ^= k1;
        h1 = Integer.rotateLeft(h1, 13);
        h1 = h1 * 5 + 0xe6546b64;
        return h1;
    }

    // Finalization mix - force all bits of a hash block to avalanche
    private static int fmix(int h1, int length) {
        h1 ^= length;
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;
        return h1;
    }

    private static long codePointToFourUtf8Bytes(int codePoint) {
        return (((0xFL << 4) | (codePoint >>> 18)) & 0xFF)
                | ((0x80L | (0x3F & (codePoint >>> 12))) << 8)
                | ((0x80L | (0x3F & (codePoint >>> 6))) << 16)
                | ((0x80L | (0x3F & codePoint)) << 24);
    }

    private static long charToThreeUtf8Bytes(char c) {
        return (((0xF << 5) | (c >>> 12)) & 0xFF)
                | ((0x80 | (0x3F & (c >>> 6))) << 8)
                | ((0x80 | (0x3F & c)) << 16);
    }

    private static long charToTwoUtf8Bytes(char c) {
        return (((0xF << 6) | (c >>> 6)) & 0xFF) | ((0x80 | (0x3F & c)) << 8);
    }

    private static int toInt(byte value) {
        return value & 0xFF;
    }

    private static final long serialVersionUID = 0L;
}
