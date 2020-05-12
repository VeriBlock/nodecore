// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.util;

import org.veriblock.sdk.transactions.SharedConstants;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.BitSet;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
        if (hex.length() % 2 == 1) {
            hex = "0" + hex;
        }
        int len = hex.length();

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
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
        Preconditions.argument(initial.length <= length, "Invalid pad length");

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

    public static String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int pos = name.lastIndexOf(".");

        return pos == -1 ? name : name.substring(0, pos);
    }

    public static int getCurrentTimestamp() {
        return (int) Instant.now().getEpochSecond();
    }

    public static boolean verifySignature(byte[] payload, byte[] signatureBytes, byte[] publicKeyBytes) {
        try {
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(publicKey);
            signature.update(payload);

            return signature.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    public static int toInt(BitSet bitset) {
        int value = 0;
        for (int i = 0; i < bitset.length(); i++) {
            if (bitset.get(i)) {
                value += 1 << i;
            }
        }
        return value;
    }

    public static int toInt(byte[] bytes) {
        if (bytes.length > 4) throw new IllegalArgumentException();

        byte[] intBytes = new byte[4];
        System.arraycopy(bytes, 0, intBytes, 4 - bytes.length, bytes.length);

        return ByteBuffer.wrap(intBytes).getInt();
    }

    public static byte[] toByteArray(int value) {
        return new byte[]{(byte) ((value & -16777216) >> 24), (byte) ((value & 16711680) >> 16), (byte) ((value & '\uff00') >> 8), (byte) (value & 255)};
    }

    public static long toLong(byte[] bytes) {
        if (bytes.length > 8) throw new IllegalArgumentException();

        byte[] intBytes = new byte[8];
        System.arraycopy(bytes, 0, intBytes, 8 - bytes.length, bytes.length);

        return ByteBuffer.wrap(intBytes).getLong();
    }

    public static byte[] fillBytes(byte value, int count) {
        byte[] data = new byte[count];
        for (int i = 0; i < count; i++) data[i] = value;
        return data;
    }

    public static class Bytes {
        public static short readBEInt16(ByteBuffer buffer) {
            return buffer.getShort();
        }

        public static int readBEInt32(ByteBuffer buffer) {
            return buffer.getInt();
        }

        public static int readLEInt32(ByteBuffer buffer) {
            return Integer.reverseBytes(buffer.getInt());
        }

        public static void putBEInt16(ByteBuffer buffer, short value) {
            buffer.putShort(value);
        }

        public static void putBEInt32(ByteBuffer buffer, int value) {
            buffer.putInt(value);
        }

        public static void putBEBytes(ByteBuffer buffer, byte[] value) {
            buffer.put(value);
        }

        public static void putLEBytes(ByteBuffer buffer, byte[] value) {
            buffer.put(reverseBytes(value));
        }

        public static void putLEInt32(ByteBuffer buffer, int value) {
            buffer.putInt(Integer.reverseBytes(value));
        }
    }


    public static boolean isValidPort(int port) {
        return port > 0 && port < 65536;
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    /**
     * Encodes the provided hexadecimal string into a byte array.
     *
     * @param s The hexadecimal string
     * @return A byte array consisting of the bytes within the hexadecimal String
     */
    public static byte[] hexToBytes(String s) {
        if (s == null) {
            throw new IllegalArgumentException("hexToBytes cannot be called with a null String!");
        }

        if (!isHex(s)) {
            throw new IllegalArgumentException("hexToBytes cannot be called with a non-hex String (called with " + s + ")!");
        }

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Note: fails in 2038.
     *
     * @return Conversion from current time/UNIX epoch (ms) to seconds since Jan 1, 1970.
     */
    public static int getCurrentTimeSeconds() {
        return (int) Instant.now().getEpochSecond();
    }

    /**
     * Determines if some number of second have elapsed since a given time
     *
     * @param origin The timestamp to measure
     * @param delta  The amount of time to measure has elapsed
     * @return Whether delta has elapsed since origin
     */
    public static boolean hasElapsed(int origin, int delta) {
        return origin + delta < getCurrentTimeSeconds();
    }

    /**
     * Determines whether a provided String is a bit String (a String comprised of all zeroes and ones).
     *
     * @param toTest String to test
     * @return Whether toTest is a bit String
     */
    public static boolean isBitString(String toTest) {
        if (toTest == null) {
            return false;
        }
        for (int i = 0; i < toTest.length(); i++) {
            char character = toTest.charAt(i);
            if (character != '0' && character != '1') {
                return false;
            }
        }

        return true;
    }

    public static boolean isAlphabetic(char toTest) {
        return ('a' <= toTest && 'z' >= toTest) | ('A' <= toTest && 'Z' >= toTest);
    }

    public static boolean isAlphabetic(String toTest) {
        for (int i = 0; i < toTest.length(); i++) {
            if (!isAlphabetic(toTest.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNumeric(char toTest) {
        return ('0' <= toTest && '9' >= toTest);
    }

    public static boolean isNumeric(String toTest) {
        for (int i = 0; i < toTest.length(); i++) {
            if (!isNumeric(toTest.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAlphanumeric(char toTest) {
        return isAlphabetic(toTest) | isNumeric(toTest);
    }

    public static boolean isAlphanumeric(String toTest) {
        return isAlphabetic(toTest) | isNumeric(toTest);
    }

    /**
     * Encodes the provided byte array into an upper-case hexadecimal string.
     *
     * @param bytes The byte array to encode
     * @return A String of the hexadecimal representation of the provided byte array
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytesToHex cannot be called with a null byte array!");
        }

        /* Two hex characters always represent one byte */
        char[] hex = new char[bytes.length << 1];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            hex[j++] = SharedConstants.HEX_ALPHABET_ARRAY[(0xF0 & bytes[i]) >>> 4];
            hex[j++] = SharedConstants.HEX_ALPHABET_ARRAY[(0x0F & bytes[i])];
        }
        return new String(hex);
    }

    public static boolean isHex(String toTest) {
        if (toTest == null) {
            throw new IllegalArgumentException("isHex cannot be called with a null String!");
        }

        for (char c : toTest.toCharArray()) {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                    continue;
                default:
                    return false;
            }
        }

        return true;
    }

    /**
     * Creates the smallest possible byte array that can hold the number of input.
     * <p>
     * For example:
     * Calling this method with 0L will return a byte array: {0x00}
     * Calling this method with 1L will return a byte array: {0x01}
     * Calling this method with 255L will return a byte array: {0xFF}
     * Calling this method with 256L will return a byte array: {0x01, 0x00}
     * Calling this method with -1L will return a byte array: {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF}
     *
     * @param input The long to convert to a trimmed byte array
     * @return The shortest possible byte array which stores the number provided by input
     */
    public static byte[] trimmedByteArrayFromLong(long input) {
        int x = 8;
        do {
            if ((input >> ((x - 1) * 8)) != 0) {
                break;
            }
            x--;
        } while (x > 1);

        byte[] trimmedByteArray = new byte[x];
        for (int i = 0; i < x; i++) {
            trimmedByteArray[x - i - 1] = (byte) (input);
            input >>= 8;
        }

        return trimmedByteArray;
    }

    public static byte[] signMessageWithPrivateKey(byte[] message, PrivateKey privateKey) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
        if (message == null) {
            throw new IllegalArgumentException("signMessageWithPrivateKey cannot be called with a null message to sign!");
        }

        if (privateKey == null) {
            throw new IllegalArgumentException("signMessageWithPrivateKey cannot be called with a null private key!");
        }

        Signature signature = Signature.getInstance("SHA256withECDSA");

        signature.initSign(privateKey);
        signature.update(message);

        return signature.sign();
    }

    public static byte[] trimmedByteArrayFromInteger(int input) {
        return trimmedByteArrayFromLong(input);
    }

    /**
     * Encodes the provided byte array into a base-58 string.
     *
     * @param bytes The byte array to encode
     * @return A String of the base-58 representation of the provided byte array
     */
    public static String bytesToBase58(byte[] bytes) {
        return Base58.encode(bytes);
    }

    /**
     * Encodes the provided byte array into a base-59 string.
     *
     * @param bytes The byte array to encode
     * @return A String of the base-59 representation of the provided byte array
     */
    public static String bytesToBase59(byte[] bytes) {
        return Base59.encode(bytes);
    }

    public static byte[] base58ToBytes(String base58) {
        return Base58.decode(base58);
    }

    public static byte[] base59ToBytes(String base59) {
        return Base59.decode(base59);
    }

    public static byte[] base64ToBytes(String base64) {
        return java.util.Base64.getDecoder().decode(base64);
    }

    public static String bytesToBase64(byte[] bytes) {
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }


    /**
     * Returns a reversed copy of the provided byte[]. While some use cases would certainly benefit from an in-place reversal
     * of the bytes in the array itself rather than creation of a new byte[], always creating a new, reversed array is the
     * easiest way to avoid unexpected modification.
     *
     * @param toReverse The byte[] to reverse
     * @return A reversed copy of toReverse
     */
    public static byte[] flip(byte[] toReverse) {
        if (toReverse == null) {
            throw new IllegalArgumentException("flip cannot be called with a null byte array!");
        }

        int left = 0;
        int right = toReverse.length - 1;

        byte[] reversed = new byte[toReverse.length];

        while (left < right) {
            byte tmp = toReverse[left];
            reversed[left++] = toReverse[right];
            reversed[right--] = tmp;
        }

        if (left == right) {
            reversed[left] = toReverse[right];
        }

        return reversed;
    }

    /**
     * Returns a byte[] containing the data from two arrays concatenated together
     *
     * @param first  The first array to concatenate
     * @param second The second array to concatenate
     * @return A byte[] holding all bytes, in order, from first, followed by all bytes, in order, from second.
     */
    public static byte[] concat(byte[] first, byte[] second) {
        if (first == null) {
            throw new IllegalArgumentException("concat cannot be called with a null first byte array!");
        }
        if (second == null) {
            throw new IllegalArgumentException("concat cannot be called with a null second byte array!");
        }


        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static boolean isPositiveOrZero(double toTest) {
        return toTest >= 0;
    }

    public static boolean isPositiveOrZero(float toTest) {
        return toTest >= 0;
    }

    public static boolean isPositiveOrZero(long toTest) {
        return toTest >= 0;
    }

    public static boolean isPositiveOrZero(int toTest) {
        return toTest >= 0;
    }

    public static boolean isPositiveOrZero(short toTest) {
        return toTest >= 0;
    }

    public static boolean isPositiveOrZero(char toTest) {
        return toTest >= 0;
    }

    public static boolean isPositiveOrZero(byte toTest) {
        return toTest >= 0;
    }


    public static boolean isPositive(double toTest) {
        return toTest > 0;
    }

    public static boolean isPositive(float toTest) {
        return toTest > 0;
    }

    public static boolean isPositive(long toTest) {
        return toTest > 0;
    }

    public static boolean isPositive(int toTest) {
        return toTest > 0;
    }

    public static boolean isPositive(short toTest) {
        return toTest > 0;
    }

    public static boolean isPositive(char toTest) {
        return toTest > 0;
    }

    public static boolean isPositive(byte toTest) {
        return toTest > 0;
    }

    /**
     * Tests whether a provided String can be successfully parsed to a positive or zero (>-1) long.
     *
     * @param toTest String to attempt to parse to a positive or zero (>-1) long
     * @return Whether or not the provided String can be successfully parsed to a positive of zero (>-1) long
     */
    public static boolean isPositiveOrZeroLong(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            long parsed = Long.parseLong(toTest);
            if (parsed >= 0)
                return true; /* Didn't throw an exception, and is > 0 */
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    /**
     * Tests whether a provided String can be successfully parsed to a positive (>0) long.
     *
     * @param toTest String to attempt to parse to a positive (>0) long
     * @return Whether or not the provided String can be successfully parsed to a positive (>0) long
     */
    public static boolean isPositiveLong(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            long parsed = Long.parseLong(toTest);
            if (parsed > 0)
                return true; /* Didn't throw an exception, and is > 0 */
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    /**
     * Tests whether a provided String can be successfully parsed to a negative (<0) long.
     *
     * @param toTest String to attempt to parse to a negative (<0) long
     * @return Whether or not the provided String can be successfully parsed to a negative (<0) long
     */
    public static boolean isNegativeLong(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            long parsed = Long.parseLong(toTest);
            if (parsed < 0)
                return true; /* Didn't throw an exception, and is < 0 */
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    /**
     * Tests whether a provided String can be successfully parsed to a negative or zero (<=0) long.
     *
     * @param toTest String to attempt to parse to a negative (<=0) long
     * @return Whether or not the provided String can be successfully parsed to a negative or zero (<=0) long
     */
    public static boolean isNegativeOrZeroLong(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            long parsed = Long.parseLong(toTest);
            if (parsed <= 0)
                return true; /* Didn't throw an exception, and is < 0 */
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isLong(String toTest) {
        try {
            Long.parseLong(toTest);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isInteger(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            Integer.parseInt(toTest);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isPositiveInteger(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            int parsed = Integer.parseInt(toTest);
            if (parsed > 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isPositiveOrZeroInteger(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            int parsed = Integer.parseInt(toTest);
            if (parsed >= 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isNegativeOrZeroInteger(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            int parsed = Integer.parseInt(toTest);
            if (parsed <= 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isNegativeInteger(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            int parsed = Integer.parseInt(toTest);
            if (parsed < 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isShort(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            Short.parseShort(toTest);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isPositiveShort(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            short parsed = Short.parseShort(toTest);
            if (parsed > 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isPositiveOrZeroShort(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            short parsed = Short.parseShort(toTest);
            if (parsed >= 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isNegativeOrZeroShort(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            short parsed = Short.parseShort(toTest);
            if (parsed <= 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isNegativeShort(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            short parsed = Short.parseShort(toTest);
            if (parsed < 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isFloat(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            Float.parseFloat(toTest);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isPositiveFloat(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            float parsed = Float.parseFloat(toTest);
            if (parsed > 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isPositiveOrZeroFloat(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            float parsed = Float.parseFloat(toTest);
            if (parsed >= 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isNegativeOrZeroFloat(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            float parsed = Float.parseFloat(toTest);
            if (parsed <= 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isNegativeFloat(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            float parsed = Float.parseFloat(toTest);
            if (parsed < 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isDouble(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            Double.parseDouble(toTest);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isPositiveDouble(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            double parsed = Double.parseDouble(toTest);
            if (parsed > 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isPositiveOrZeroDouble(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            double parsed = Double.parseDouble(toTest);
            if (parsed >= 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isNegativeOrZeroDouble(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            double parsed = Double.parseDouble(toTest);
            if (parsed <= 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isNegativeDouble(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            double parsed = Double.parseDouble(toTest);
            if (parsed < 0)
                return true;
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static boolean isBigInteger(String toTest) {
        if (toTest == null) {
            return false;
        }
        try {
            new BigInteger(toTest, 10);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Converts an integer to a byte[] in big-endian.
     *
     * @param input The integer to convert into a byte[]
     * @return The byte[] representing the provided integer
     */
    public static byte[] intToByteArray(int input) {
        return new byte[]{
                (byte) ((input & 0xFF000000) >> 24),
                (byte) ((input & 0x00FF0000) >> 16),
                (byte) ((input & 0x0000FF00) >> 8),
                (byte) ((input & 0x000000FF)),
        };
    }

    /**
     * Converts a big-endian byte[] too an integer
     *
     * @param input The byte array to interpret as an integer
     * @return The integer resulting from interpreting the provided byte array
     */
    public static int byteArrayToInt(byte[] input) {
        if (input.length != 4) {
            throw new IllegalArgumentException("byteArrayToInt cannot be called with an input which isn't of length 4!");
        }

        return (((int) input[0] & 0xFF) << 24) |
                (((int) input[1] & 0xFF) << 16) |
                (((int) input[2] & 0xFF) << 8) |
                (((int) input[3] & 0xFF));
    }


    /**
     * Converts the given four bytes to big-endian integer
     *
     * @return The integer resulting from interpreting the provided byte array
     */
    public static int bytesToInt(byte b1, byte b2, byte b3, byte b4) {
        return b1 << 24 |
                (b2 & 0xFF) << 16 |
                (b3 & 0xFF) << 8 |
                (b4 & 0xFF);
    }

    /**
     * Converts a long to a byte[] in big-endian.
     *
     * @param input The long to convert into a byte[]
     * @return The byte[] representing the provided long
     */
    public static byte[] longToByteArray(long input) {
        return new byte[]{
                (byte) ((input & 0xFF00000000000000l) >> 56),
                (byte) ((input & 0x00FF000000000000l) >> 48),
                (byte) ((input & 0x0000FF0000000000l) >> 40),
                (byte) ((input & 0x000000FF00000000l) >> 32),
                (byte) ((input & 0x00000000FF000000l) >> 24),
                (byte) ((input & 0x0000000000FF0000l) >> 16),
                (byte) ((input & 0x000000000000FF00l) >> 8),
                (byte) ((input & 0x00000000000000FFl)),
        };
    }

    public static boolean byteArraysAreEqual(byte[] first, byte[] second) {
        if (first == null) {
            throw new IllegalArgumentException("byteArraysAreEqual cannot be called with a null first byte array!");
        }
        if (second == null) {
            throw new IllegalArgumentException("byteArraysAreEqual cannot be called with a null second byte array!");
        }

        if (first.length != second.length)
            return false;

        for (int i = 0; i < first.length; i++)
            if (first[i] != second[i])
                return false;

        return true;
    }

    public static boolean appendToNamedFile(String message, String fileName) {
        if (message == null) {
            throw new IllegalArgumentException("appendToNamedFile cannot be called with a null message!");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("appendToNamedFile cannot be called with a null fileName!");
        }

        try {
            if (!new File(fileName).exists()) {
                try (PrintWriter out = new PrintWriter(new File(fileName))) {
                    out.println("");
                }
            }
            Files.write(Paths.get(fileName), (message + "\n").getBytes(), StandardOpenOption.APPEND);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static String zeroPadHexToVeriBlockBlockHash(String toPad) {
        return zeroPad(toPad, SharedConstants.VBLAKE_HASH_OUTPUT_SIZE_BYTES * 2); // Multiply by two to account for 2 hex characters = 1 byte
    }

    /**
     * Pads a hexadecimal String with starting zeros such that it is exactly the requested length. If toPad is already
     * longer than the requested length, it will be returned as-is!
     *
     * @param toPad Hexadecimal string to pad
     * @param size  Length to zero-pad to
     * @return A zero-padded version of toPad
     */
    public static String zeroPad(String toPad, int size) {
        if (toPad == null) {
            throw new IllegalArgumentException("zeroPad cannot be called with a null String to pad!");
        }

        if (!isHex(toPad)) {
            throw new IllegalArgumentException("toPad must be a hexadecimal String!");
        }

        if (toPad.length() >= size) {
            return toPad;
        }

        int difference = size - toPad.length();

        StringBuilder padded = new StringBuilder();
        for (; difference > 0; difference--) {
            padded.append("0");
        }
        padded.append(toPad);

        return padded.toString();
    }

    public static String flipHex(String hex) {
        byte[] bytes = hexToBytes(hex);
        return bytesToHex(flip(bytes));
    }

    public static String formatHttpDate(Date d) {
        final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
        final TimeZone GMT = TimeZone.getTimeZone("GMT");
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
        formatter.setTimeZone(GMT);
        return formatter.format(d);
    }

    public static String formatAtomicLongWithDecimal(long toFormat) {
        boolean isNegative = toFormat < 0;
        String result = "" + (isNegative ? -1 * toFormat : toFormat);
        while (result.length() < 8) {
            result = "0" + result;
        }

        int spotForDecimal = result.length() - 8;
        result = (isNegative ? "-" : "") + result.substring(0, spotForDecimal) + "." + result.substring(spotForDecimal);
        if (result.charAt(0) == '.') {
            result = "0" + result;
        }
        return result;
    }

    public static int countChar(String toSearch, char toFind) {
        if (toSearch == null) {
            throw new IllegalArgumentException("countChar cannot be called with a null String!");
        }
        int count = 0;
        for (int i = 0; i < toSearch.length(); i++) {
            if (toSearch.charAt(i) == toFind) {
                count++;
            }
        }

        return count;
    }

    private static final String DECIMAL_NUMBER_CHARACTERS = "-0123456789.";

    public static boolean isDecimalNumber(String number) {
        if (number == null) {
            throw new IllegalArgumentException("isDecimalNumber cannot be called with a null String!");
        }
        for (int i = 0; i < number.length(); i++) {
            if (!DECIMAL_NUMBER_CHARACTERS.contains("" + number.charAt(i))) {
                throw new IllegalArgumentException("isDecimalNumber cannot be called with a non-decimal number (" + number + ")!");
            }
        }

        if (countChar(number, '.') > 1) {
            throw new IllegalArgumentException("isDecimalNumber cannot be called with a String with more than one decimal point (" + number + ")");
        }

        return true;
    }

    public static long convertDecimalCoinToAtomicLong(String toConvert) {
        if (!isDecimalNumber(toConvert)) {
            throw new IllegalArgumentException("convertDecimalCoinToAtomicLong cannot be called with a non-decimal String (" + toConvert + ")!");
        }

        if (!toConvert.contains(".")) {
            toConvert = toConvert + ".";
        }

        if (toConvert.charAt(0) == '.') {
            toConvert = "0" + toConvert;
        }

        int numCharactersAfterDecimal = toConvert.length() - toConvert.indexOf(".") - 1;

        if (numCharactersAfterDecimal > 8) {
            throw new IllegalArgumentException("convertDecimalCoinToAtomicLong cannot be called with a String with more than 8 numbers after the decimal point!");
        }

        toConvert = toConvert.replace(".", "");

        for (int i = 8; i > numCharactersAfterDecimal; i--) {
            toConvert += "0";
        }

        return Long.parseLong(toConvert);
    }

    public static byte[] stringToBytes(String input) {
        return input.getBytes(StandardCharsets.UTF_8);
    }

    public static String bytesToString(byte[] input) {
        return new String(input, StandardCharsets.UTF_8);
    }

    public static String bytesToString(byte[] input, String defaultValue) {
        if (input == null || input.length == 0) return defaultValue;

        return bytesToString(input);
    }
}
