package org.veriblock.alt.plugins.util

import java.io.ByteArrayOutputStream
import java.io.IOException

private const val ALPHABET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
private val GENERATOR = intArrayOf(0x3B6A57B2, 0x26508E6D, 0x1EA119FA, 0x3D4233DD, 0x2A1462B3)
private const val CHECKSUM_LEN = 6

fun segwitToBech32(humanPart: String, witVer: Int, witProg: ByteArray): String {
    require(witVer in 0..16) {
        "Invalid witness version"
    }
    require(witProg.size in 2..40) {
        "Invalid witness program length"
    }

    // Create buffer of 5-bit groups
    val data = ByteArrayOutputStream() // Every element is uint5
    data.write(witVer) // uint5

    // Variables/constants for bit processing
    val IN_BITS = 8
    val OUT_BITS = 5
    var inputIndex = 0
    var bitBuffer = 0 // Topmost bitBufferLen bits are valid; remaining lower bits are zero
    var bitBufferLen = 0 // Always in the range [0, 12]

    // Repack all 8-bit bytes into 5-bit groups, adding padding
    while (inputIndex < witProg.size || bitBufferLen > 0) {
        if (bitBufferLen < OUT_BITS) {
            if (inputIndex < witProg.size) {  // Read a byte
                bitBuffer = bitBuffer or (witProg[inputIndex].toInt() and 0xFF shl 32 - IN_BITS - bitBufferLen)
                inputIndex++
                bitBufferLen += IN_BITS
            } else  // Create final padding
                bitBufferLen = OUT_BITS
        }
        // Write a 5-bit group
        data.write(bitBuffer ushr 32 - OUT_BITS) // uint5
        bitBuffer = bitBuffer shl OUT_BITS
        bitBufferLen -= OUT_BITS
    }
    return bitGroupsToBech32(humanPart, data.toByteArray())
}

fun bitGroupsToBech32(humanPart: String, data: ByteArray): String {
    val human = humanPart.toCharArray()
    checkHumanReadablePart(human)
    for (b in data) {
        require(b.toInt() ushr 5 == 0) {
            "Expected 5-bit groups"
        }
    }
    require(human.size + 1 + data.size + 6 <= 90) {
        "Output too long"
    }

    // Compute checksum
    val checksum = try {
        val temp = expandHumanReadablePart(human) // Every element is uint5
        temp.write(data)
        temp.write(ByteArray(CHECKSUM_LEN))
        polymod(temp.toByteArray()) xor 1
    } catch (e: IOException) {
        throw AssertionError(e) // Impossible
    }

    // Encode to base-32
    val sb = StringBuilder(humanPart).append('1')
    for (b in data) sb.append(ALPHABET[b.toInt()])
    for (i in 0 until CHECKSUM_LEN) {
        val b = checksum ushr (CHECKSUM_LEN - 1 - i) * 5 and 0x1F
        sb.append(ALPHABET[b])
    }
    return sb.toString()
}


fun checkHumanReadablePart(s: CharArray) {
    val n = s.size
    require(!(n < 1 || n > 83)) {
        "Invalid length of human-readable part string"
    }
    for (c in s) {
        require(!(c.code < 33 || c.code > 126)) {
            "Invalid character in human-readable part string"
        }
        require(c !in 'A'..'Z') {
            "Human-readable part string must be lowercase"
        }
    }
}

private fun expandHumanReadablePart(s: CharArray): ByteArrayOutputStream {
    val result = ByteArrayOutputStream()
    for (c in s) {
        result.write(c.code ushr 5)
    }
    result.write(0)
    for (c in s) {
        result.write(c.code and 0x1F)
    }
    return result
}

private fun polymod(data: ByteArray): Int {
    var result = 1
    for (b in data) {
        val x = result ushr 25
        result = result and (1 shl 25) - 1 shl 5 or b.toInt()
        for (i in GENERATOR.indices) {
            result = result xor (x ushr i and 1) * GENERATOR[i]
        }
    }
    return result
}
