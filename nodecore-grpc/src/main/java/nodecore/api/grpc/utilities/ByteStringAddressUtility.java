// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.grpc.utilities;

import com.google.protobuf.ByteString;
import org.veriblock.core.utilities.AddressUtility;

public class ByteStringAddressUtility {
    /**
     * Attempts to automatically parse the appropriate (standard or multisig) address type automatically from a protobuf
     * ByteString. If the provided ByteString is not a valid standard or multisig address, then this will throw an exception.
     *
     * @param toParse ByteString to parse into a Standard or Multisig address
     * @return A valid Standard or Multisig address parsed from the provided ByteString
     */
    public static String parseProperAddressTypeAutomatically(ByteString toParse) {
        if (toParse == null) {
            throw new IllegalArgumentException("parseProperAddressTypeAutomatically cannot be called with a " +
                    "null toParse ByteString!");
        }
        if (isByteStringValidAddress(toParse)) {
            return ByteStringUtility.byteStringToBase58(toParse);
        }
        else if (isByteStringValidMultisigAddress(toParse)) {
            return ByteStringUtility.byteStringToBase59(toParse);
        }
        else {
            throw new IllegalArgumentException("parseProperAddressTypeAutomatically cannot be called with an " +
                    "invalid ByteString!");
        }
    }

    /**
     * Automatically generates a proper protobuf ByteString from the provided standard or multisig address.
     *
     * @param address A valid standard or multisig address to generate the protobuf ByteString from
     * @return A ByteString generated from the provided address String
     */
    public static ByteString createProperByteStringAutomatically(String address) {
        if (address == null) {
            throw new IllegalArgumentException("createProperByteStringAutomatically cannot be called with a " +
                    "null address String!");
        }

        if (AddressUtility.isValidStandardAddress(address)) {
            return ByteStringUtility.base58ToByteString(address);
        }
        else if (AddressUtility.isValidMultisigAddress(address)) {
            return ByteStringUtility.base59ToByteString(address);
        }
        else {
            throw new IllegalArgumentException("createProperByteStringAutomatically cannot be called with an invalid" +
                    " address (" + address + ")!");
        }
    }

    /**
     * Determines whether the provided protobuf ByteString represents a valid standard address.
     *
     * @param toTest ByteString which may or may not represent a valid standard address
     * @return Whether the provided ByteString represents a valid standard address
     */
    public static boolean isByteStringValidAddress(ByteString toTest) {
        if (toTest == null) {
            throw new IllegalArgumentException("isByteStringValidAddress cannot be called with a " +
                    "null toTest ByteString!");
        }

        String potentialAddress = ByteStringUtility.byteStringToBase58(toTest);

        return AddressUtility.isValidStandardAddress(potentialAddress);
    }

    /**
     * Determines whether the provided protobuf ByteString represents a valid multisig address.
     *
     * @param toTest ByteString which may or may not represent a valid multisig address
     * @return Whether the provided ByteString represents a valid multisig address
     */
    public static boolean isByteStringValidMultisigAddress(ByteString toTest) {
        if (toTest == null) {
            throw new IllegalArgumentException("isByteStringValidMultisigAddress cannot be called with a " +
                    "null toTest ByteString!");
        }

        String potentialMultisigAddress = ByteStringUtility.byteStringToBase59(toTest);

        return AddressUtility.isValidMultisigAddress(potentialMultisigAddress);
    }
}
