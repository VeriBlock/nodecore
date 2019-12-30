package org.veriblock.core.utilities.extensions

import org.veriblock.core.bitcoinj.Base58
import org.veriblock.core.bitcoinj.Base59
import java.util.*

/**
 * Determines whether a provided String is a bit String (a String comprised of all zeroes and ones).
 * @param this@isBitString String to test
 * @return Whether toTest is a bit String
 */
fun String.isBitString(): Boolean {
    for (character in this) {
        if (character != '0' && character != '1') {
            return false
        }
    }
    return true
}

private const val hexAlphabet = "0123456789ABCDEF"
private val hexArray = hexAlphabet.toCharArray()

/**
 * Encodes the provided hexadecimal string into a byte array.
 *
 * @param s The hexadecimal string
 * @return A byte array consisting of the bytes within the hexadecimal String
 */
fun String.asHexBytes(): ByteArray {
    require(isHex()) {
        "non-hex String cannot be converted to hex (called with $this)!"
    }
    val len = length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(get(i), 16) shl 4) + Character.digit(get(i + 1), 16)).toByte()
        i += 2
    }
    return data
}

/**
 * Encodes the provided byte array into an upper-case hexadecimal string.
 *
 * @param this@bytesToHex The byte array to encode
 * @return A String of the hexadecimal representation of the provided byte array
 */
fun ByteArray.toHex(): String {
    /* Two hex characters always represent one byte */
    val hex = CharArray(size shl 1)
    var i = 0
    var j = 0
    while (i < size) {
        val v: Int = get(i).toInt()
        hex[j++] = hexArray[0xF0 and v ushr 4]
        hex[j++] = hexArray[0x0F and v]
        i++
    }
    return String(hex)
}

fun String.isHex(): Boolean = toUpperCase().toCharArray().all {
    it in hexAlphabet
}

/**
 * Encodes the provided byte array into a base-58 string.
 *
 * @param this@bytesToBase58 The byte array to encode
 * @return A String of the base-58 representation of the provided byte array
 */
fun ByteArray.toBase58(): String {
    return Base58.encode(this)
}

/**
 * Encodes the provided byte array into a base-59 string.
 *
 * @param this@bytesToBase59 The byte array to encode
 * @return A String of the base-59 representation of the provided byte array
 */
fun ByteArray.toBase59(): String {
    return Base59.encode(this)
}

fun ByteArray.toBase64(): String {
    return Base64.getEncoder().encodeToString(this)
}

fun String.asBase58Bytes(): ByteArray {
    return Base58.decode(this)
}

fun String.asBase59Bytes(): ByteArray {
    return Base59.decode(this)
}

fun String.asBase64Bytes(): ByteArray {
    return Base64.getDecoder().decode(this)
}


/**
 * Returns a reversed copy of the provided byte[]. While some use cases would certainly benefit from an in-place reversal
 * of the bytes in the array itself rather than creation of a new byte[], always creating a new, reversed array is the
 * easiest way to avoid unexpected modification.
 *
 * @param this@flip The byte[] to reverse
 * @return A reversed copy of toReverse
 */
fun ByteArray.flip(): ByteArray {
    var left = 0
    var right = size - 1
    val reversed = ByteArray(size)
    while (left < right) {
        val tmp = this[left]
        reversed[left++] = this[right]
        reversed[right--] = tmp
    }
    if (left == right) {
        reversed[left] = this[right]
    }
    return reversed
}

/**
 * Pads a hexadecimal String with starting zeros such that it is exactly the requested length. If toPad is already
 * longer than the requested length, it will be returned as-is!
 *
 * @param this@zeroPad Hexadecimal string to pad
 * @param size Length to zero-pad to
 * @return A zero-padded version of toPad
 */
fun String.zeroPad(size: Int): String {
    require(this.isHex()) {
        "toPad must be a hexadecimal String!"
    }
    if (length >= size) {
        return this
    }
    var difference = size - length
    val padded = StringBuilder()
    while (difference > 0) {
        padded.append("0")
        difference--
    }
    padded.append(this)
    return padded.toString()
}

fun String.flipHex(): String {
    return asHexBytes().flip().toHex()
}
