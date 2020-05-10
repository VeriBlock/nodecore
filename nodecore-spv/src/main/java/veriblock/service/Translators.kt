// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.service

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.Utility
import java.nio.charset.StandardCharsets

object Translators {
    @JvmStatic
    fun base64ToHex(element: JsonElement): JsonElement {
        val bytes = Utility.base64ToBytes(element.asString)
        return JsonPrimitive(Utility.bytesToHex(bytes))
    }

    @JvmStatic
    fun hexToBase64(element: JsonElement): JsonElement {
        val bytes = Utility.hexToBytes(element.asString)
        return JsonPrimitive(Utility.bytesToBase64(bytes))
    }

    @JvmStatic
    fun base64ToProperAddressType(element: JsonElement): JsonElement {
        val bytes = Utility.base64ToBytes(element.asString)
        return if (isByteStringValidAddress(bytes)) {
            JsonPrimitive(Utility.bytesToBase58(bytes))
        } else if (isByteStringValidMultisigAddress(bytes)) {
            JsonPrimitive(Utility.bytesToBase59(bytes))
        } else {
            throw IllegalArgumentException("hexToProperAddressType cannot be called with an invalid input!")
        }
    }

    @JvmStatic
    fun arbitraryAddressTypeToBase64(element: JsonElement): JsonElement {
        val address = element.asString
        return if (AddressUtility.isValidStandardAddress(address)) {
            val bytes = Utility.base58ToBytes(address)
            JsonPrimitive(Utility.bytesToBase64(bytes))
        } else if (AddressUtility.isValidMultisigAddress(address)) {
            val bytes = Utility.base59ToBytes(address)
            JsonPrimitive(Utility.bytesToBase64(bytes))
        } else {
            throw IllegalArgumentException("arbitraryAddressTypeToHex cannot be called with an invalid input ($address)!")
        }
    }

    @JvmStatic
    fun base64ToAscii(element: JsonElement): JsonElement {
        val bytes = Utility.base64ToBytes(element.asString)
        return JsonPrimitive(String(bytes, StandardCharsets.US_ASCII))
    }

    @JvmStatic
    fun asciiToBase64(element: JsonElement): JsonElement {
        val bytes = element.asString.toByteArray(StandardCharsets.US_ASCII)
        return JsonPrimitive(Utility.bytesToBase64(bytes))
    }

    @JvmStatic
    fun base64ToUtf8(element: JsonElement): JsonElement {
        val bytes = Utility.base64ToBytes(element.asString)
        return JsonPrimitive(String(bytes, StandardCharsets.UTF_8))
    }

    @JvmStatic
    fun utf8ToBase64(element: JsonElement): JsonElement {
        val bytes = element.asString.toByteArray(StandardCharsets.UTF_8)
        return JsonPrimitive(Utility.bytesToBase64(bytes))
    }

    @JvmStatic
    fun noOp(element: JsonElement): JsonElement {
        return element
    }

    private fun isByteStringValidAddress(bytes: ByteArray?): Boolean {
        return if (bytes == null) {
            throw IllegalArgumentException("isByteStringValidAddress cannot be called with a null toTest ByteString!")
        } else {
            val potentialAddress = Utility.bytesToBase58(bytes)
            AddressUtility.isValidStandardAddress(potentialAddress)
        }
    }

    private fun isByteStringValidMultisigAddress(bytes: ByteArray?): Boolean {
        return if (bytes == null) {
            throw IllegalArgumentException("isByteStringValidMultisigAddress cannot be called with a null toTest ByteString!")
        } else {
            val potentialMultisigAddress = Utility.bytesToBase59(bytes)
            AddressUtility.isValidMultisigAddress(potentialMultisigAddress)
        }
    }
}
