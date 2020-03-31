// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.common

import org.bitcoinj.core.Base58
import org.bitcoinj.core.Block
import org.bitcoinj.core.Coin
import org.bitcoinj.utils.MonetaryFormat
import org.quartz.CronScheduleBuilder
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Arrays
import java.util.UUID

/**
 * A lightweight Utility class comprised entirely of static methods for low-level encoding/manipulation/numerical tasks.
 */
object Utility {
    private const val hexAlphabet = "0123456789ABCDEF"
    private val hexArray = hexAlphabet.toCharArray()

    /**
     * Encodes the provided byte array into an upper-case hexadecimal string.
     *
     * @param bytes The byte array to encode
     * @return A String of the hexadecimal representation of the provided byte array
     */
    @JvmStatic
    fun bytesToHex(bytes: ByteArray): String {
        /* Two hex characters always represent one byte */
        val hex = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v: Int = bytes[i].toInt() and 0xFF
            hex[i * 2] = hexArray[v ushr 4]
            hex[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hex)
    }

    /**
     * Encodes the provided hexadecimal string into a byte array.
     *
     * @param s The hexadecimal string
     * @return A byte array consisting of the bytes within the hexadecimal String
     */
    fun hexToBytes(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun isHex(toTest: String): Boolean {
        for (c in toTest.toLowerCase().toCharArray()) {
            if (!('0' <= c && c <= '9' || 'a' <= c && c <= 'f')) {
                return false
            }
        }
        return true
    }

    /**
     * Returns a reversed copy of the provided byte[]. While some use cases would certainly benefit from an in-place reversal
     * of the bytes in the array itself rather than creation of a new byte[], always creating a new, reversed array is the
     * easiest way to avoid unexpected modification.
     *
     * @param toReverse The byte[] to reverse
     * @return A reversed copy of toReverse
     */
    fun flip(toReverse: ByteArray): ByteArray {
        val reversed = ByteArray(toReverse.size)
        for (i in toReverse.indices) {
            reversed[i] = toReverse[toReverse.size - 1 - i]
        }
        return reversed
    }

    fun flipHex(hex: String): String {
        val bytes = hexToBytes(hex)
        return bytesToHex(flip(bytes))
    }

    /**
     * Returns a byte[] containing the data from two arrays concatenated together
     *
     * @param first  The first array to concatenate
     * @param second The second array to concatenate
     * @return A byte[] holding all bytes, in order, from first, followed by all bytes, in order, from second.
     */
    fun concat(first: ByteArray, second: ByteArray): ByteArray {
        val result = ByteArray(first.size + second.size)
        System.arraycopy(first, 0, result, 0, first.size)
        System.arraycopy(second, 0, result, first.size, second.size)
        return result
    }

    /**
     * Tests whether a provided String can be successfully parsed to a positive or zero (>-1) long.
     *
     * @param toTest String to attempt to parse to a positive or zero (>-1) long
     * @return Whether or not the provided String can be successfully parsed to a positive of zero (>-1) long
     */
    fun isPositiveOrZeroLong(toTest: String): Boolean {
        try {
            val parsed = toTest.toLong()
            if (parsed >= 0) {
                return true /* Didn't throw an exception, and is > 0 */
            }
        } catch (e: Exception) {
        }
        return false
    }

    /**
     * Tests whether a provided String can be successfully parsed to a positive (>0) long.
     *
     * @param toTest String to attempt to parse to a positive (>0) long
     * @return Whether or not the provided String can be successfully parsed to a positive (>0) long
     */
    fun isPositiveLong(toTest: String): Boolean {
        try {
            val parsed = toTest.toLong()
            if (parsed > 0) {
                return true /* Didn't throw an exception, and is > 0 */
            }
        } catch (e: Exception) {
        }
        return false
    }

    /**
     * Tests whether a provided String can be successfully parsed to a negative (<0) long.
     *
     * @param toTest String to attempt to parse to a negative (<0) long
     * @return Whether or not the provided String can be successfully parsed to a negative (<0) long
     */
    fun isNegativeLong(toTest: String): Boolean {
        try {
            val parsed = toTest.toLong()
            if (parsed < 0) {
                return true /* Didn't throw an exception, and is < 0 */
            }
        } catch (e: Exception) {
        }
        return false
    }

    @JvmStatic
    fun isInteger(toTest: String): Boolean {
        try {
            toTest.toInt()
            return true
        } catch (e: Exception) {
        }
        return false
    }

    fun isPositiveInteger(toTest: String): Boolean {
        try {
            val parsed = toTest.toInt()
            if (parsed > 0) {
                return true
            }
        } catch (e: Exception) {
        }
        return false
    }

    fun isBigInteger(toTest: String?): Boolean {
        return try {
            BigInteger(toTest, 10)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Converts an integer to a byte[] in big-endian.
     *
     * @param input The integer to convert into a byte[]
     * @return The byte[] representing the provided integer
     */
    fun intToByteArray(input: Int): ByteArray {
        return byteArrayOf(
            (input and -0x1000000 shr 24).toByte(), (input and 0x00FF0000 shr 16).toByte(),
            (input and 0x0000FF00 shr 8).toByte(),
            (input and 0x000000FF).toByte()
        )
    }

    /**
     * Converts a long to a byte[] in big-endian.
     *
     * @param input The long to convert into a byte[]
     * @return The byte[] representing the provided integer
     */
    fun longToByteArray(input: Long): ByteArray {
        return byteArrayOf(
            (input and -0x1000000 shr 24).toByte(), (input and 0x00FF0000 shr 16).toByte(),
            (input and 0x0000FF00 shr 8).toByte(),
            (input and 0x000000FF).toByte()
        )
    }

    fun byteArraysAreEqual(first: ByteArray, second: ByteArray): Boolean {
        if (first.size != second.size) {
            return false
        }
        for (i in first.indices) {
            if (first[i] != second[i]) {
                return false
            }
        }
        return true
    }

    fun generateOperationId(): String {
        val id = UUID.randomUUID()
        return id.toString().substring(0, 8)
    }

    fun serializeBlock(block: Block): ByteArray {
        return Arrays.copyOfRange(block.bitcoinSerialize(), 0, 80)
    }

    @JvmStatic
    fun amountToCoin(amount: BigDecimal): Coin {
        val satoshis: Long = amount.movePointRight(8).longValueExact()
        return Coin.valueOf(satoshis)
    }

    fun bytesToBase58(bytes: ByteArray?): String {
        return Base58.encode(bytes)
    }

    fun formatAtomicLongWithDecimal(toFormat: Long): String {
        val isNegative = toFormat < 0
        var result = "" + if (isNegative) -1 * toFormat else toFormat
        while (result.length < 8) {
            result = "0$result"
        }
        val spotForDecimal = result.length - 8
        result = (if (isNegative) "-" else "") + result.substring(0, spotForDecimal) + "." + result.substring(spotForDecimal)
        if (result[0] == '.') {
            result = "0$result"
        }
        return result
    }

    @JvmStatic
    fun isValidCronExpression(value: String?): Boolean {
        return try {
            CronScheduleBuilder.cronSchedule(value)
            true
        } catch (e: RuntimeException) {
            false
        }
    }

    private val BTC_FORMAT = MonetaryFormat.BTC.minDecimals(8).repeatOptionalDecimals(
        8, 0
    ).postfixCode()

    /**
     * Returns the value as a 0.12 type string. More digits after the decimal place will be used
     * if necessary, but two will always be present.
     */
    @JvmStatic
    fun formatBTCFriendlyString(coin: Coin?): String {
        return BTC_FORMAT.format(coin).toString()
    }
}
