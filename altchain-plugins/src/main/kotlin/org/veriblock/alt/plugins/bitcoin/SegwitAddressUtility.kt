package org.veriblock.alt.plugins.bitcoin

import org.veriblock.core.utilities.extensions.toHex
import kotlin.experimental.or

object SegwitAddressUtility {
    private const val BECH32_CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    fun generatePayoutScriptFromSegwitAddress(segwitAddress: String): ByteArray {
        val hrp = segwitAddress.substringBefore("1")
        // P2WPKH: 33 + 6 + hrp.length() + 1
        //  P2WSH: 53 + 6 + hrp.length() + 1
        require(segwitAddress.length == 39 + hrp.length + 1 || segwitAddress.length == 59 + hrp.length + 1) {
            "generatePayoutScriptFromSegwitAddress cannot be called with an address which isn't the length of 39 or 59 + hrp + 1!" +
                " Provided address: $segwitAddress, hrp: $hrp"
        }
        require(segwitAddress[hrp.length] == '1') {
            "generatePayoutScriptFromSegwitAddress cannot be called with an address that" +
                " doesn't have the character '1' separating the hrp from the remainder of the address!"
        }
        val addressWithoutHRP = segwitAddress.substring(hrp.length)
        var addressDataSection = addressWithoutHRP.substring(1, addressWithoutHRP.length - 6) // Remove checksum
        //val addressChecksum = segwitAddress.substring(segwitAddress.length - 6)
        require(addressDataSection[0] == 'q') {
            "generatePayoutScriptFromSegwitAddress cannot be called with an address that" +
                " doesn't contain a 'q' immediately after the last '1' in the String!"
        }
        addressDataSection = addressDataSection.substring(1) // Remove initial 'q'
        val decodedData = decodeBech32(addressDataSection)
        val script = ByteArray(decodedData.size + 2)
        script[0] = 0x00.toByte()
        script[1] = decodedData.size.toByte()
        decodedData.copyInto(script, destinationOffset = 2)
        return script
    }

    private fun decodeBech32(inputBech32: String): ByteArray {
        if (inputBech32.isEmpty()) {
            return byteArrayOf() // Return empty byte array
        }
        val bech32String = inputBech32.toLowerCase()
        // Check to ensure all characters are bech32
        for (i in bech32String.indices) {
            require(BECH32_CHARSET.contains(bech32String[i])) {
                "decodeBech32 cannot be called with a non-bech32 String ($bech32String has character '${bech32String[i]}' at index $i!"
            }
        }
        val decoded = ByteArray(bech32String.length * 5 / 8 + if (bech32String.length * 5 % 8 == 0) 0 else 1)
        for (i in bech32String.indices) {
            val decodedValue = BECH32_CHARSET.indexOf(bech32String[i]) and 0b00011111
            val byteIndex = (i + 1) * 5 / 8
            when (i % 8) {
                0 -> {
                    decoded[byteIndex] = decoded[byteIndex] or ((decodedValue shl 3) and 0b11111111).toByte()
                }
                1 -> {
                    decoded[byteIndex - 1] = decoded[byteIndex - 1] or ((decodedValue ushr 2) and 0b00000111).toByte()
                    decoded[byteIndex] = decoded[byteIndex] or ((decodedValue and 0b00000011) shl 6).toByte()
                }
                2 -> {
                    decoded[byteIndex] = decoded[byteIndex] or (decodedValue shl 1).toByte()
                }
                3 -> {
                    decoded[byteIndex - 1] = decoded[byteIndex - 1] or ((decodedValue and 0b00010000) ushr 4).toByte()
                    decoded[byteIndex] = decoded[byteIndex] or ((decodedValue and 0b00001111) shl 4).toByte()
                }
                4 -> {
                    decoded[byteIndex - 1] = decoded[byteIndex - 1] or ((decodedValue and 0b00011110) shr 1).toByte()
                    decoded[byteIndex] = decoded[byteIndex] or ((decodedValue and 0b00000001) shl 7).toByte()
                }
                5 -> {
                    decoded[byteIndex] = decoded[byteIndex] or ((decodedValue shl 2) and 0b01111100).toByte()
                }
                6 -> {
                    decoded[byteIndex - 1] = decoded[byteIndex - 1] or ((decodedValue and 0b00011000) shr 3).toByte()
                    decoded[byteIndex] = decoded[byteIndex] or ((decodedValue and 0b00000111) shl 5).toByte()
                }
                7 -> {
                    decoded[byteIndex - 1] = decoded[byteIndex - 1] or (decodedValue and 0b00011111).toByte()
                }
            }
        }

        // Chop off an ending zero, if it exists
        return if (decoded.last() == 0x00.toByte()) {
            decoded.copyOfRange(0, decoded.size - 1)
        } else {
            decoded
        }
    }
}

fun main() {
    println(SegwitAddressUtility.generatePayoutScriptFromSegwitAddress("tb1q0ad0rgrtuha9nu7y9g02pfc5asttmhwz53hr4l").toHex())
}
