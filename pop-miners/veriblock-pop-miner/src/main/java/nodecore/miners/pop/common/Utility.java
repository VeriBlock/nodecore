// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.common;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.MonetaryFormat;
import org.quartz.CronScheduleBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;

/**
 * A lightweight Utility class comprised entirely of static methods for low-level encoding/manipulation/numerical tasks.
 */
public class Utility {
    private static final String hexAlphabet = "0123456789ABCDEF";
    private static final char[] hexArray = hexAlphabet.toCharArray();

    /**
     * Encodes the provided byte array into an upper-case hexadecimal string.
     *
     * @param bytes The byte array to encode
     * @return A String of the hexadecimal representation of the provided byte array
     */
    public static String bytesToHex(byte[] bytes) {
        /* Two hex characters always represent one byte */
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = hexArray[v >>> 4];
            hex[i * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hex);
    }

    /**
     * Encodes the provided hexadecimal string into a byte array.
     *
     * @param s The hexadecimal string
     * @return A byte array consisting of the bytes within the hexadecimal String
     */
    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static boolean isHex(String toTest) {
        for (char c : toTest.toLowerCase().toCharArray()) {
            if (!(('0' <= c && c <= '9') || ('a' <= c && c <= 'f'))) {
                return false;
            }
        }
        return true;
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
        byte[] reversed = new byte[toReverse.length];

        for (int i = 0; i < toReverse.length; i++) {
            reversed[i] = toReverse[toReverse.length - 1 - i];
        }
        return reversed;
    }

    public static String flipHex(String hex) {
        byte[] bytes = hexToBytes(hex);
        return bytesToHex(flip(bytes));
    }

    /**
     * Returns a byte[] containing the data from two arrays concatenated together
     *
     * @param first  The first array to concatenate
     * @param second The second array to concatenate
     * @return A byte[] holding all bytes, in order, from first, followed by all bytes, in order, from second.
     */
    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * Tests whether a provided String can be successfully parsed to a positive or zero (>-1) long.
     *
     * @param toTest String to attempt to parse to a positive or zero (>-1) long
     * @return Whether or not the provided String can be successfully parsed to a positive of zero (>-1) long
     */
    public static boolean isPositiveOrZeroLong(String toTest) {
        try {
            long parsed = Long.parseLong(toTest);
            if (parsed >= 0) {
                return true; /* Didn't throw an exception, and is > 0 */
            }
        } catch (Exception e) {
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
        try {
            long parsed = Long.parseLong(toTest);
            if (parsed > 0) {
                return true; /* Didn't throw an exception, and is > 0 */
            }
        } catch (Exception e) {
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
        try {
            long parsed = Long.parseLong(toTest);
            if (parsed < 0) {
                return true; /* Didn't throw an exception, and is < 0 */
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean isInteger(String toTest) {
        try {
            Integer.parseInt(toTest);
            return true;
        } catch (Exception e) {
        }

        return false;
    }

    public static boolean isPositiveInteger(String toTest) {
        try {
            int parsed = Integer.parseInt(toTest);
            if (parsed > 0) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean isBigInteger(String toTest) {
        try {
            new BigInteger(toTest, 10);
            return true;
        } catch (Exception e) {
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
        byte[] bytes = new byte[]{
            (byte) ((input & 0xFF000000) >> 24), (byte) ((input & 0x00FF0000) >> 16), (byte) ((input & 0x0000FF00) >> 8),
            (byte) ((input & 0x000000FF)),
        };
        return bytes;
    }

    /**
     * Converts a long to a byte[] in big-endian.
     *
     * @param input The long to convert into a byte[]
     * @return The byte[] representing the provided integer
     */
    public static byte[] longToByteArray(long input) {
        byte[] bytes = new byte[]{
            (byte) ((input & 0xFF000000) >> 24), (byte) ((input & 0x00FF0000) >> 16), (byte) ((input & 0x0000FF00) >> 8),
            (byte) ((input & 0x000000FF)),
        };
        return bytes;
    }

    public static boolean byteArraysAreEqual(byte[] first, byte[] second) {
        if (first.length != second.length) {
            return false;
        }

        for (int i = 0; i < first.length; i++) {
            if (first[i] != second[i]) {
                return false;
            }
        }

        return true;
    }

    public static String generateOperationId() {
        UUID id = UUID.randomUUID();
        return id.toString().substring(0, 8);
    }

    public static byte[] serializeBlock(Block block) {
        return Arrays.copyOfRange(block.bitcoinSerialize(), 0, 80);
    }

    public static Coin amountToCoin(BigDecimal amount) {
        long satoshis = amount.movePointRight(8).longValue();
        return Coin.valueOf(satoshis);
    }

    public static String bytesToBase58(byte[] bytes) {
        return Base58.encode(bytes);
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

    public static boolean isValidCronExpression(String value) {
        try {
            CronScheduleBuilder.cronSchedule(value);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static final MonetaryFormat BTC_FORMAT = MonetaryFormat.BTC.minDecimals(8).repeatOptionalDecimals(8, 0).postfixCode();

    /**
     * Returns the value as a 0.12 type string. More digits after the decimal place will be used
     * if necessary, but two will always be present.
     */
    public static String formatBTCFriendlyString(Coin coin) {
        return BTC_FORMAT.format(coin).toString();
    }
}
