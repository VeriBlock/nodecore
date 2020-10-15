package org.veriblock.core.crypto

import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toHex
import java.nio.ByteBuffer

const val VBK_HASH_LENGTH = 24
const val VBK_PREVIOUS_BLOCK_HASH_LENGTH = 12
const val VBK_PREVIOUS_KEYSTONE_HASH_LENGTH = 9

val VBK_EMPTY_HASH = VbkHash(ByteArray(VBK_HASH_LENGTH))

sealed class AnyVbkHash(
    val bytes: ByteArray
) {
    open fun trimToPreviousBlockSize(): PreviousBlockVbkHash {
        check(bytes.size >= VBK_PREVIOUS_BLOCK_HASH_LENGTH) {
            "Length should be higher than previous block length"
        }
        val trimmedBytes = trimBytes(VBK_PREVIOUS_BLOCK_HASH_LENGTH)
        return PreviousBlockVbkHash(trimmedBytes)
    }

    open fun trimToPreviousKeystoneSize(): PreviousKeystoneVbkHash {
        check(bytes.size >= VBK_PREVIOUS_KEYSTONE_HASH_LENGTH) {
            "Length should be higher than previous keystone length"
        }
        val trimmedBytes = trimBytes(VBK_PREVIOUS_KEYSTONE_HASH_LENGTH)
        return PreviousKeystoneVbkHash(trimmedBytes)
    }

    override fun equals(other: Any?): Boolean {
        return other is AnyVbkHash && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        val length = bytes.size
        return Utility.fromBytes(bytes[length - 4], bytes[length - 3], bytes[length - 2], bytes[length - 1])
    }

    override fun toString(): String =
        bytes.toHex()
}

private fun AnyVbkHash.trimBytes(size: Int): ByteArray {
    return ByteArray(size).apply {
        System.arraycopy(bytes, bytes.size - size, this, 0, size)
    }
}

class VbkHash(
    bytes: ByteArray
) : AnyVbkHash(bytes) {
    init {
        check(bytes.size != VBK_HASH_LENGTH) {
            "Trying to create a VBK hash with invalid amount of bytes: ${bytes.size} (${bytes.toHex()})"
        }
    }
}

class PreviousBlockVbkHash(
    bytes: ByteArray
) : AnyVbkHash(bytes) {
    init {
        check(bytes.size != VBK_PREVIOUS_BLOCK_HASH_LENGTH) {
            "Trying to create a previous block VBK hash with invalid amount of bytes: ${bytes.size} (${bytes.toHex()})"
        }
    }
}

class PreviousKeystoneVbkHash(
    bytes: ByteArray
) : AnyVbkHash(bytes) {
    init {
        check(bytes.size != VBK_PREVIOUS_KEYSTONE_HASH_LENGTH) {
            "Trying to create a previous keystone VBK hash with invalid amount of bytes: ${bytes.size} (${bytes.toHex()})"
        }
    }
}

fun ByteArray.asVbkHash(): VbkHash = VbkHash(this)
fun ByteArray.asVbkPreviousBlockHash(): PreviousBlockVbkHash = PreviousBlockVbkHash(this)
fun ByteArray.asVbkPreviousKeystoneHash(): PreviousKeystoneVbkHash = PreviousKeystoneVbkHash(this)

fun String.asVbkHash(): VbkHash = asHexBytes().asVbkHash()
fun String.asVbkPreviousBlockHash(): PreviousBlockVbkHash = asHexBytes().asVbkPreviousBlockHash()
fun String.asVbkPreviousKeystoneHash(): PreviousKeystoneVbkHash = asHexBytes().asVbkPreviousKeystoneHash()

fun ByteBuffer.readVbkHash(): VbkHash = ByteArray(VBK_HASH_LENGTH).let {
    get(it)
    VbkHash(it)
}
fun ByteBuffer.readVbkPreviousBlockHash(): PreviousBlockVbkHash = ByteArray(VBK_PREVIOUS_BLOCK_HASH_LENGTH).let {
    get(it)
    PreviousBlockVbkHash(it)
}
fun ByteBuffer.readVbkPreviousKeystoneHash(): PreviousKeystoneVbkHash = ByteArray(VBK_PREVIOUS_KEYSTONE_HASH_LENGTH).let {
    get(it)
    PreviousKeystoneVbkHash(it)
}

fun ByteArray.asAnyVbkHash(): AnyVbkHash = when (size) {
    VBK_HASH_LENGTH -> asVbkHash()
    VBK_PREVIOUS_BLOCK_HASH_LENGTH -> asVbkPreviousBlockHash()
    VBK_PREVIOUS_KEYSTONE_HASH_LENGTH -> asVbkPreviousKeystoneHash()
    else -> error("Trying to create an arbitrary VBK hash with invalid amount of bytes: $size (${toHex()})")
}

fun String.asAnyVbkHash(): AnyVbkHash = asHexBytes().asAnyVbkHash()
