// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.core.crypto

import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.flip
import org.veriblock.core.utilities.extensions.toHex
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class Sha256Hash(
    val bytes: ByteArray,
    val length: Int
) : Comparable<Sha256Hash> {

    init {
        require(bytes.size == length) {
            "Invalid Sha256 hash: ${bytes.toHex()}"
        }
    }

    /**
     * Returns the last four bytes of the wrapped hash. This should be unique enough to be a suitable hash code even for
     * blocks, where the goal is to try and get the first bytes to be zeros (i.e. the value as a big integer lower
     * than the target value).
     */
    override fun hashCode(): Int {
        // Use the last 4 bytes, not the first 4 which are often zeros in Bitcoin.
        return Utility.fromBytes(bytes[length - 4], bytes[length - 3], bytes[length - 2], bytes[length - 1])
    }

    override fun toString(): String {
        return bytes.toHex()
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     */
    fun toBigInteger(): BigInteger {
        return BigInteger(1, bytes)
    }

    /**
     * Returns a reversed copy of the internal byte array.
     */
    fun reversedBytes(): ByteArray = bytes.flip()
    fun reversed(): Sha256Hash = Sha256Hash(bytes.flip(), bytes.size)

    fun trim(length: Int): Sha256Hash {
        require(bytes.size >= length) { "Invalid trim length" }
        if (this.length == length) return this
        val trimmed = bytes.copyOfRange(0, length)
        return wrap(trimmed, length)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other == null || javaClass != other.javaClass) false else bytes.contentEquals((other as Sha256Hash).bytes)
    }

    override fun compareTo(other: Sha256Hash): Int {
        for (i in BITCOIN_LENGTH - 1 downTo 0) {
            val thisByte: Int = bytes[i].toInt() and 0xff
            val otherByte: Int = other.bytes[i].toInt() and 0xff
            if (thisByte > otherByte) return 1
            if (thisByte < otherByte) return -1
        }
        return 0
    }

    companion object {
        const val BITCOIN_LENGTH = 32 // bytes
        const val VERIBLOCK_MERKLE_ROOT_LENGTH = 16
        val ZERO_HASH = wrap(ByteArray(BITCOIN_LENGTH))

        /**
         * Creates a new instance that wraps the given hash value.
         *
         * @param rawHashBytes the raw hash bytes to wrap
         * @return a new instance
         * @throws IllegalArgumentException if the given array length is not exactly 32
         */
        @JvmStatic
        fun wrap(rawHashBytes: ByteArray): Sha256Hash {
            return Sha256Hash(rawHashBytes, BITCOIN_LENGTH)
        }

        /**
         * Creates a new instance that wraps the given hash value.
         *
         * @param rawHashBytes the raw hash bytes to wrap
         * @param length the expected length;
         * @return a new instance
         * @throws IllegalArgumentException if the given array length is not exactly length
         */
        @JvmStatic
        fun wrap(rawHashBytes: ByteArray, length: Int): Sha256Hash {
            return Sha256Hash(rawHashBytes, length)
        }

        /**
         * Creates a new instance that wraps the given hash value (represented as a hex string).
         *
         * @param hexString a hash value represented as a hex string
         * @return a new instance
         * @throws IllegalArgumentException if the given string is not a valid
         * hex string, or if it does not represent exactly 32 bytes
         */
        @JvmStatic
        fun wrap(hexString: String): Sha256Hash {
            return wrap(hexString.asHexBytes())
        }

        /**
         * Creates a new instance that wraps the given hash value (represented as a hex string).
         *
         * @param hexString a hash value represented as a hex string
         * @param length the expected length;
         * @return a new instance
         * @throws IllegalArgumentException if the given string is not a valid
         * hex string, or if it does not represent exactly length
         */
        @JvmStatic
        fun wrap(hexString: String, length: Int): Sha256Hash {
            return wrap(hexString.asHexBytes(), length)
        }

        /**
         * Creates a new instance that wraps the given hash value, but with byte order reversed.
         *
         * @param rawHashBytes the raw hash bytes to wrap
         * @return a new instance
         * @throws IllegalArgumentException if the given array length is not exactly 32
         */
        @JvmStatic
        fun wrapReversed(rawHashBytes: ByteArray): Sha256Hash {
            return wrap(rawHashBytes.flip())
        }

        /**
         * Creates a new instance that wraps the given hash value, but with byte order reversed.
         *
         * @param rawHashBytes the raw hash bytes to wrap
         * @param length the length of this hash
         * @return a new instance
         * @throws IllegalArgumentException if the given array length is not exactly 32
         */
        fun wrapReversed(rawHashBytes: ByteArray, length: Int): Sha256Hash {
            return wrap(rawHashBytes.flip(), length)
        }

        /**
         * Creates a new instance that wraps the given hash value, but with byte order reversed.
         *
         * @param hexString a hash value represented as a hex string
         * @return a new instance
         * @throws IllegalArgumentException if the given array length is not exactly 32
         */
        fun wrapReversed(hexString: String): Sha256Hash {
            return wrap(hexString.asHexBytes().flip())
        }

        /**
         * Creates a new instance containing the calculated (one-time) hash of the given bytes.
         *
         * @param contents the bytes on which the hash value is calculated
         * @return a new instance containing the calculated (one-time) hash
         */
        @JvmStatic
        fun of(contents: ByteArray): Sha256Hash {
            return wrap(hash(contents))
        }

        /**
         * Creates a new instance containing the calculated (one-time) hash of the given bytes.
         *
         * @param first the bytes on which the hash value is calculated
         * @param second the bytes on which the hash value is calculated
         * @return a new instance containing the calculated (one-time) hash
         */
        fun of(first: ByteArray, second: ByteArray): Sha256Hash {
            return wrap(hash(first, 0, first.size, second, 0, second.size))
        }

        /**
         * Creates a new instance containing the hash of the calculated hash of the given bytes.
         *
         * @param contents the bytes on which the hash value is calculated
         * @return a new instance containing the calculated (two-time) hash
         */
        fun twiceOf(contents: ByteArray): Sha256Hash {
            return wrap(hashTwice(contents))
        }

        /**
         * Creates a new instance containing the hash of the calculated hash of the given bytes.
         */
        fun twiceOf(first: ByteArray, second: ByteArray): Sha256Hash {
            return wrap(hashTwice(first, 0, first.size, second, 0, second.size))
        }

        fun extract(buffer: ByteBuffer, length: Int = BITCOIN_LENGTH, endianness: ByteOrder = ByteOrder.LITTLE_ENDIAN): Sha256Hash {
            val dest = ByteArray(length)
            buffer[dest]
            return if (endianness == ByteOrder.LITTLE_ENDIAN) {
                wrapReversed(dest, length)
            } else {
                wrap(dest, length)
            }
        }

        /**
         * Returns a new SHA-256 MessageDigest instance.
         *
         * This is a convenience method which wraps the checked
         * exception that can never occur with a RuntimeException.
         *
         * @return a new SHA-256 MessageDigest instance
         */
        private fun newDigest(): MessageDigest {
            return try {
                MessageDigest.getInstance("SHA-256")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e) // Can't happen.
            }
        }

        /**
         * Calculates the SHA-256 hash of the given byte range.
         *
         * @param input the array containing the bytes to hash
         * @param offset the offset within the array of the bytes to hash
         * @param length the number of bytes to hash
         * @return the hash (in big-endian order)
         */
        @JvmStatic
        @JvmOverloads
        fun hash(input: ByteArray, offset: Int = 0, length: Int = input.size): ByteArray {
            val digest = newDigest()
            digest.update(input, offset, length)
            return digest.digest()
        }

        /**
         * Calculates the SHA-256 hash of the given byte range.
         *
         * @param first the array containing the bytes to hash
         * @param offset1 the offset within the array of the bytes to hash
         * @param length1 the number of bytes to hash
         * @return the hash (in big-endian order)
         */
        fun hash(first: ByteArray, offset1: Int, length1: Int, second: ByteArray, offset2: Int, length2: Int): ByteArray {
            val digest = newDigest()
            digest.update(first, offset1, length1)
            digest.update(second, offset2, length2)
            return digest.digest()
        }

        /**
         * Calculates the SHA-256 hash of the given byte range,
         * and then hashes the resulting hash again.
         *
         * @param input the array containing the bytes to hash
         * @param offset the offset within the array of the bytes to hash
         * @param length the number of bytes to hash
         * @return the double-hash (in big-endian order)
         */
        fun hashTwice(input: ByteArray, offset: Int = 0, length: Int = input.size): ByteArray {
            val digest = newDigest()
            digest.update(input, offset, length)
            return digest.digest(digest.digest())
        }

        /**
         * Calculates the hash of hash on the given byte ranges. This is equivalent to
         * concatenating the two ranges and then passing the result to [.hashTwice].
         */
        fun hashTwice(
            input1: ByteArray, offset1: Int, length1: Int,
            input2: ByteArray, offset2: Int, length2: Int
        ): ByteArray {
            val digest = newDigest()
            digest.update(input1, offset1, length1)
            digest.update(input2, offset2, length2)
            return digest.digest(digest.digest())
        }
    }
}
