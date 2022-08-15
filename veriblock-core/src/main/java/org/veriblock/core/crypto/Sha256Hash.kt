// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
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
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

const val BITCOIN_HASH_LENGTH = 32 // bytes
const val TRUNCATED_MERKLE_ROOT_LENGTH = 16

private val EMPTY_ARRAY = ByteArray(BITCOIN_HASH_LENGTH)
val EMPTY_BITCOIN_HASH = EMPTY_ARRAY.asBtcHash()
val EMPTY_VBK_TX = EMPTY_ARRAY.asVbkTxId()

val EMPTY_TRUNCATED_MERKLE_ROOT = ByteArray(TRUNCATED_MERKLE_ROOT_LENGTH).asTruncatedMerkleRoot()

open class Sha256Hash(
    val bytes: ByteArray
) : Comparable<Sha256Hash> {

    val length: Int get() = bytes.size

    /**
     * Returns the last four bytes of the wrapped hash. This should be unique enough to be a suitable hash code even for
     * blocks, where the goal is to try and get the first bytes to be zeros (i.e. the value as a big integer lower
     * than the target value).
     */
    override fun hashCode(): Int {
        // Use the last 4 bytes, not the first 4 which are often zeros in Bitcoin.
        return Utility.fromBytes(bytes[bytes.size - 4], bytes[bytes.size - 3], bytes[bytes.size - 2], bytes[bytes.size - 1])
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
    fun reversed(): Sha256Hash = Sha256Hash(bytes.flip())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other == null || javaClass != other.javaClass) false else bytes.contentEquals((other as Sha256Hash).bytes)
    }

    override fun compareTo(other: Sha256Hash): Int {
        for (i in BITCOIN_HASH_LENGTH - 1 downTo 0) {
            val thisByte: Int = bytes[i].toInt() and 0xff
            val otherByte: Int = other.bytes[i].toInt() and 0xff
            if (thisByte > otherByte) return 1
            if (thisByte < otherByte) return -1
        }
        return 0
    }
}

class BtcHash(bytes: ByteArray) : Sha256Hash(bytes) {
    init {
        check(bytes.size == BITCOIN_HASH_LENGTH) {
            "Trying to create a BTC hash with invalid amount of bytes: ${bytes.size} (${bytes.toHex()})"
        }
    }
}

class VbkTxId(bytes: ByteArray) : Sha256Hash(bytes) {
    init {
        check(bytes.size == BITCOIN_HASH_LENGTH) {
            "Trying to create a VBK Transaction id with invalid amount of bytes: ${bytes.size} (${bytes.toHex()})"
        }
    }
}

class MerkleRoot(bytes: ByteArray) : Sha256Hash(bytes) {
    init {
        check(bytes.size == BITCOIN_HASH_LENGTH) {
            "Trying to create a merkle root hash with invalid amount of bytes: ${bytes.size} (${bytes.toHex()})"
        }
    }

    fun truncate(): TruncatedMerkleRoot {
        val trimmed = bytes.copyOfRange(0, TRUNCATED_MERKLE_ROOT_LENGTH)
        return trimmed.asTruncatedMerkleRoot()
    }
}

class TruncatedMerkleRoot(bytes: ByteArray) : Sha256Hash(trimBytes(bytes, TRUNCATED_MERKLE_ROOT_LENGTH)) {
    init {
        check(bytes.size == TRUNCATED_MERKLE_ROOT_LENGTH || bytes.size == 24) {
            "Trying to create a truncated merkle root hash with invalid amount of bytes: ${bytes.size} (${bytes.toHex()})"
        }
    }
}

fun ByteArray.asSha256Hash() = Sha256Hash(this)
fun ByteArray.asBtcHash() = BtcHash(this)
fun ByteArray.asVbkTxId() = VbkTxId(this)
fun ByteArray.asMerkleRoot() = MerkleRoot(this)
fun ByteArray.asTruncatedMerkleRoot() = TruncatedMerkleRoot(this)

fun String.asSha256Hash() = asHexBytes().asSha256Hash()
fun String.asBtcHash() = asHexBytes().asBtcHash()
fun String.asVbkTxId() = asHexBytes().asVbkTxId()
fun String.asMerkleRoot() = asHexBytes().asMerkleRoot()
fun String.asTruncatedMerkleRoot() = asHexBytes().asTruncatedMerkleRoot()

/**
 * Creates a new instance containing the hash of the calculated hash of the given bytes.
 *
 * @param contents the bytes on which the hash value is calculated
 * @return a new instance containing the calculated (two-time) hash
 */
fun btcHashOf(contents: ByteArray): BtcHash {
    return doubleSha256HashOf(contents).flip().asBtcHash()
}

/**
 * Creates a new instance containing the calculated (one-time) hash of the given bytes.
 *
 * @param first the bytes on which the hash value is calculated
 * @param second the bytes on which the hash value is calculated
 * @return a new instance containing the calculated (one-time) hash
 */
fun merkleRootHashOf(first: ByteArray, second: ByteArray): MerkleRoot {
    return sha256HashOf(first, 0, first.size, second, 0, second.size).asMerkleRoot()
}

fun ByteBuffer.readBtcHash(): BtcHash {
    val dest = ByteArray(BITCOIN_HASH_LENGTH)
    get(dest)
    return dest.flip().asBtcHash()
}

fun ByteBuffer.readBtcMerkleRoot(): MerkleRoot {
    val dest = ByteArray(BITCOIN_HASH_LENGTH)
    get(dest)
    return dest.flip().asMerkleRoot()
}

fun ByteBuffer.readTruncatedMerkleRoot(): TruncatedMerkleRoot {
    val dest = ByteArray(TRUNCATED_MERKLE_ROOT_LENGTH)
    get(dest)
    return dest.asTruncatedMerkleRoot()
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
fun sha256HashOf(input: ByteArray, offset: Int = 0, length: Int = input.size): ByteArray {
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
fun sha256HashOf(first: ByteArray, offset1: Int, length1: Int, second: ByteArray, offset2: Int, length2: Int): ByteArray {
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
fun doubleSha256HashOf(input: ByteArray, offset: Int = 0, length: Int = input.size): ByteArray {
    val digest = newDigest()
    digest.update(input, offset, length)
    return digest.digest(digest.digest())
}

/**
 * Calculates the hash of hash on the given byte ranges. This is equivalent to
 * concatenating the two ranges and then passing the result to [.hashTwice].
 */
fun doubleSha256HashOf(
    input1: ByteArray, offset1: Int, length1: Int,
    input2: ByteArray, offset2: Int, length2: Int
): ByteArray {
    val digest = newDigest()
    digest.update(input1, offset1, length1)
    digest.update(input2, offset2, length2)
    return digest.digest(digest.digest())
}

/**
 * Creates a new instance containing the hash of the calculated hash of the given bytes.
 */
fun doubleSha256HashOf(first: ByteArray, second: ByteArray): ByteArray {
    return doubleSha256HashOf(first, 0, first.size, second, 0, second.size)
}
