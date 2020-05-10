// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.util;

import org.veriblock.core.utilities.Utility;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VBlakeHash;

import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {
    public static final char[] HEX = "0123456789ABCDEF".toCharArray();

    public static byte[] reverseBytes(byte[] input) {
        int length = input.length;

        byte[] output = new byte[length];
        for (int i = 0; i < length; i++) {
            output[i] = input[length - (i + 1)];
        }

        return output;
    }

    public static byte[] decodeHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    public static String encodeHex(byte[] bytes) {
        /* Two hex characters always represent one byte */
        char[] hex = new char[bytes.length << 1];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            hex[j++] = HEX[(0xF0 & bytes[i]) >>> 4];
            hex[j++] = HEX[(0x0F & bytes[i])];
        }
        return new String(hex);
    }

    public static int fromBytes(byte b1, byte b2, byte b3, byte b4) {
        return b1 << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
    }

    public static byte[] toBytes(BigInteger value, int size) {
        byte[] dest = new byte[size];
        byte[] src = value.toByteArray();

        int length = Math.min(src.length, size);
        int destPos = Math.max(size - src.length, 0);
        System.arraycopy(src, 0, dest, destPos, length);

        return dest;
    }

    public static String leftPad(char[] initial, char pad, int length) {
        Checks.argument(initial, arg -> arg.length <= length);

        char[] chars = new char[length];
        int offset = length - initial.length;
        for (int i = 0; i < length; i++) {
            if (i >= offset) {
                chars[i] = initial[i - offset];
            } else {
                chars[i] = pad;
            }
        }

        return new String(chars);
    }

    public static Sha256Hash hash(Sha256Hash left, Sha256Hash right) {
        byte[] bytes = Sha256Hash.hash(Utility.concat(left.getBytes(), right.getBytes()));
        return Sha256Hash.wrap(bytes, bytes.length);
    }

    public static boolean matches(Sha256Hash first, Sha256Hash other) {
        int sharedLength = Math.min(first.getBytes().length, other.getBytes().length);
        for (int i = 0; i < sharedLength; i++) {
            if (first.getBytes()[i] != other.getBytes()[i]) return false;
        }

        return true;
    }

    public static String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int pos = name.lastIndexOf(".");

        return pos == -1 ? name : name.substring(0, pos);
    }

    public static class Bytes {
        public static short readBEInt16(ByteBuffer buffer) {
            buffer.order(ByteOrder.BIG_ENDIAN);
            return buffer.getShort();
        }

        public static int readBEInt32(ByteBuffer buffer) {
            buffer.order(ByteOrder.BIG_ENDIAN);
            return buffer.getInt();
        }

        public static VBlakeHash readVBlakeHash(ByteBuffer buffer, int size) {
            buffer.order(ByteOrder.BIG_ENDIAN);

            byte[] dest = new byte[size];
            buffer.get(dest, 0, size);

            return VBlakeHash.wrap(dest, size);
        }

        public static Sha256Hash readBESha256Hash(ByteBuffer buffer, int size) {
            buffer.order(ByteOrder.BIG_ENDIAN);

            byte[] dest = new byte[size];
            buffer.get(dest, 0, size);

            return Sha256Hash.wrap(dest, size);
        }

        public static int readLEInt32(ByteBuffer buffer) {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return buffer.getInt();
        }

        public static Sha256Hash readLEHash(ByteBuffer buffer) {
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            byte[] dest = new byte[Sha256Hash.BITCOIN_LENGTH];
            buffer.get(dest, 0, Sha256Hash.BITCOIN_LENGTH);

            return Sha256Hash.wrapReversed(dest);
        }

        public static void putBEInt16(ByteBuffer buffer, short value) {
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putShort(value);
        }

        public static void putBEInt32(ByteBuffer buffer, int value) {
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(value);
        }

        public static void putBEHash(ByteBuffer buffer, Sha256Hash value) {
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.put(value.getBytes());
        }

        public static void putBEHash(ByteBuffer buffer, VBlakeHash value) {
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.put(value.getBytes());
        }

        public static void putLEInt32(ByteBuffer buffer, int value) {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(value);
        }

        public static void putLEHash(ByteBuffer buffer, Sha256Hash hash) {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(hash.getReversedBytes());
        }
    }
}
