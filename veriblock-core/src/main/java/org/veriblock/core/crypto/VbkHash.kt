package org.veriblock.core.crypto

import org.veriblock.core.utilities.Utility
import org.veriblock.core.utilities.extensions.asHexBytes
import org.veriblock.core.utilities.extensions.toHex
import java.math.BigInteger
import java.nio.ByteBuffer

sealed class AnyVbkHash(
    val bytes: ByteArray
) {
    abstract fun trimToPreviousBlockSize(): PreviousBlockVbkHash

    abstract fun trimToPreviousKeystoneSize(): PreviousKeystoneVbkHash

    fun toBigInteger(): BigInteger {
        return BigInteger(1, bytes)
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

class VbkHash(bytes: ByteArray) : AnyVbkHash(bytes) {
    init {
        check(bytes.size == HASH_LENGTH) {
            "Trying to create a VBK hash with invalid amount of bytes: ${bytes.size} (${bytes.toHex()})"
        }
    }

    override fun trimToPreviousBlockSize(): PreviousBlockVbkHash =
        PreviousBlockVbkHash(trimBytes(PreviousBlockVbkHash.HASH_LENGTH))

    override fun trimToPreviousKeystoneSize(): PreviousKeystoneVbkHash =
        PreviousKeystoneVbkHash(trimBytes(PreviousKeystoneVbkHash.HASH_LENGTH))

    companion object {
        const val HASH_LENGTH = 24
        val EMPTY_HASH = VbkHash(ByteArray(HASH_LENGTH))
    }
}

class PreviousBlockVbkHash(bytes: ByteArray) : AnyVbkHash(bytes) {
    init {
        check(bytes.size == HASH_LENGTH) {
            "Trying to create a previous block VBK hash with invalid amount of bytes: ${bytes.size} (${bytes.toHex()})"
        }
    }

    override fun trimToPreviousBlockSize(): PreviousBlockVbkHash =
        this

    override fun trimToPreviousKeystoneSize(): PreviousKeystoneVbkHash =
        PreviousKeystoneVbkHash(trimBytes(PreviousKeystoneVbkHash.HASH_LENGTH))

    companion object {
        const val HASH_LENGTH = 12
        val EMPTY_HASH = PreviousBlockVbkHash(ByteArray(HASH_LENGTH))
    }
}

class PreviousKeystoneVbkHash(bytes: ByteArray) : AnyVbkHash(bytes) {
    init {
        check(bytes.size == HASH_LENGTH) {
            "Trying to create a previous keystone VBK hash with invalid amount of bytes: ${bytes.size} (${bytes.toHex()})"
        }
    }

    override fun trimToPreviousBlockSize(): PreviousBlockVbkHash =
        error("Trying to trim a previous keystone VBK hash down to a previous block VBK hash")

    override fun trimToPreviousKeystoneSize(): PreviousKeystoneVbkHash =
        this

    companion object {
        const val HASH_LENGTH = 9
        val EMPTY_HASH = PreviousKeystoneVbkHash(ByteArray(HASH_LENGTH))
    }
}

fun ByteArray.asVbkHash(): VbkHash = VbkHash(this)
fun ByteArray.asVbkPreviousBlockHash(): PreviousBlockVbkHash = PreviousBlockVbkHash(this)
fun ByteArray.asVbkPreviousKeystoneHash(): PreviousKeystoneVbkHash = PreviousKeystoneVbkHash(this)

fun String.asVbkHash(): VbkHash = asHexBytes().asVbkHash()
fun String.asVbkPreviousBlockHash(): PreviousBlockVbkHash = asHexBytes().asVbkPreviousBlockHash()
fun String.asVbkPreviousKeystoneHash(): PreviousKeystoneVbkHash = asHexBytes().asVbkPreviousKeystoneHash()

fun ByteBuffer.readVbkHash(): VbkHash = ByteArray(VbkHash.HASH_LENGTH).let {
    get(it)
    VbkHash(it)
}

fun ByteBuffer.readVbkPreviousBlockHash(): PreviousBlockVbkHash = ByteArray(PreviousBlockVbkHash.HASH_LENGTH).let {
    get(it)
    PreviousBlockVbkHash(it)
}

fun ByteBuffer.readVbkPreviousKeystoneHash(): PreviousKeystoneVbkHash = ByteArray(PreviousKeystoneVbkHash.HASH_LENGTH).let {
    get(it)
    PreviousKeystoneVbkHash(it)
}

fun ByteArray.asAnyVbkHash(): AnyVbkHash = when (size) {
    VbkHash.HASH_LENGTH -> asVbkHash()
    PreviousBlockVbkHash.HASH_LENGTH -> asVbkPreviousBlockHash()
    PreviousKeystoneVbkHash.HASH_LENGTH -> asVbkPreviousKeystoneHash()
    else -> error("Trying to create an arbitrary VBK hash with invalid amount of bytes: $size (${toHex()})")
}

fun String.asAnyVbkHash(): AnyVbkHash = asHexBytes().asAnyVbkHash()

/**
 * TODO: "Easy-going" hashes: these always return true when their last 9 bytes are equal.
 * Use case: BFI
 */
/*class EgVbkHash(bytes: ByteArray) : VbkHash(bytes) {
    override fun equals(other: Any?): Boolean {
        return other is AnyVbkHash && trimToPreviousKeystoneSize() == other.trimToPreviousKeystoneSize()
    }
}
class EgPreviousBlockVbkHash(bytes: ByteArray) : PreviousKeystoneVbkHash(bytes) {
    override fun equals(other: Any?): Boolean {
        return other is AnyVbkHash && trimToPreviousKeystoneSize() == other.trimToPreviousKeystoneSize()
    }
}
class EgPreviousKeystoneVbkHash(bytes: ByteArray) : PreviousKeystoneVbkHash(bytes) {
    override fun equals(other: Any?): Boolean {
        return other is AnyVbkHash && trimToPreviousKeystoneSize() == other.trimToPreviousKeystoneSize()
    }
}*/

// TODO: Delete me after it is no longer used
object VbkHashUtil {
    @JvmStatic
    fun wrap(bytes: ByteArray): VbkHash = VbkHash(bytes)

    @JvmStatic
    fun wrapPreviousBlockHash(bytes: ByteArray): PreviousBlockVbkHash = PreviousBlockVbkHash(bytes)

    @JvmStatic
    fun wrapPreviousKeystoneHash(bytes: ByteArray): PreviousKeystoneVbkHash = PreviousKeystoneVbkHash(bytes)

    @JvmStatic
    fun wrap(hex: String): VbkHash = wrap(hex.asHexBytes())

    @JvmStatic
    fun wrapPreviousBlockHash(hex: String): PreviousBlockVbkHash = wrapPreviousBlockHash(hex.asHexBytes())

    @JvmStatic
    fun wrapPreviousKeystoneHash(hex: String): PreviousKeystoneVbkHash = wrapPreviousKeystoneHash(hex.asHexBytes())
}
