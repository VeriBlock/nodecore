// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package veriblock.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.core.utilities.Utility;

import java.nio.charset.StandardCharsets;

public class Translators {

    public static JsonElement base64ToHex(JsonElement element) {
        byte[] bytes = Utility.base64ToBytes(element.getAsString());
        return new JsonPrimitive(Utility.bytesToHex(bytes));
    }

    public static JsonElement hexToBase64(JsonElement element) {
        byte[] bytes = Utility.hexToBytes(element.getAsString());
        return new JsonPrimitive(Utility.bytesToBase64(bytes));
    }

    public static JsonElement base64ToProperAddressType(JsonElement element) {
        byte[] bytes = Utility.base64ToBytes(element.getAsString());
        if (isByteStringValidAddress(bytes)) {
            return new JsonPrimitive(Utility.bytesToBase58(bytes));
        } else if (isByteStringValidMultisigAddress(bytes)) {
            return new JsonPrimitive(Utility.bytesToBase59(bytes));
        } else {
            throw new IllegalArgumentException("hexToProperAddressType cannot be called with an invalid input!");
        }
    }

    public static JsonElement arbitraryAddressTypeToBase64(JsonElement element) {
        String address = element.getAsString();
        if (AddressUtility.isValidStandardAddress(address)) {
            byte[] bytes = Utility.base58ToBytes(address);
            return new JsonPrimitive(Utility.bytesToBase64(bytes));
        } else if (AddressUtility.isValidMultisigAddress(address)) {
            byte[] bytes = Utility.base59ToBytes(address);
            return new JsonPrimitive(Utility.bytesToBase64(bytes));
        } else {
            throw new IllegalArgumentException("arbitraryAddressTypeToHex cannot be called with an invalid input (" + address + ")!");
        }
    }

    public static JsonElement base64ToAscii(JsonElement element) {
        byte[] bytes = Utility.base64ToBytes(element.getAsString());
        return new JsonPrimitive(new String(bytes, StandardCharsets.US_ASCII));
    }

    public static JsonElement asciiToBase64(JsonElement element) {
        byte[] bytes = element.getAsString().getBytes(StandardCharsets.US_ASCII);
        return new JsonPrimitive(Utility.bytesToBase64(bytes));
    }

    public static JsonElement base64ToUtf8(JsonElement element) {
        byte[] bytes = Utility.base64ToBytes(element.getAsString());
        return new JsonPrimitive(new String(bytes, StandardCharsets.UTF_8));
    }

    public static JsonElement utf8ToBase64(JsonElement element) {
        byte[] bytes = element.getAsString().getBytes(StandardCharsets.UTF_8);
        return new JsonPrimitive(Utility.bytesToBase64(bytes));
    }

    public static JsonElement noOp(JsonElement element) {
        return element;
    }

    private static boolean isByteStringValidAddress(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("isByteStringValidAddress cannot be called with a null toTest ByteString!");
        } else {
            String potentialAddress = Utility.bytesToBase58(bytes);
            return AddressUtility.isValidStandardAddress(potentialAddress);
        }
    }

    private static boolean isByteStringValidMultisigAddress(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("isByteStringValidMultisigAddress cannot be called with a null toTest ByteString!");
        } else {
            String potentialMultisigAddress = Utility.bytesToBase59(bytes);
            return AddressUtility.isValidMultisigAddress(potentialMultisigAddress);
        }
    }
}
