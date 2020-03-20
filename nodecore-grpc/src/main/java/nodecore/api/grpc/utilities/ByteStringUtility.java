// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.grpc.utilities;

import com.google.protobuf.ByteString;
import org.veriblock.core.utilities.Utility;


public final class ByteStringUtility {
    private ByteStringUtility(){}

    public static ByteString base58ToByteString(String value) {
        return ByteString.copyFrom(Utility.base58ToBytes(value));
    }

    public static ByteString base59ToByteString(String value) {
        return ByteString.copyFrom(Utility.base59ToBytes(value));
    }

    public static String byteStringToBase58(ByteString value) {
        return Utility.bytesToBase58(value.toByteArray());
    }

    public static String byteStringToBase59(ByteString value) {
        return Utility.bytesToBase59(value.toByteArray());
    }

    public static ByteString base64ToByteString(String value) {
        return ByteString.copyFrom(Utility.base64ToBytes(value));
    }

    public static String byteStringToBase64(ByteString value) {
        return Utility.bytesToBase64(value.toByteArray());
    }

    public static ByteString hexToByteString(String value) {
        return ByteString.copyFrom(Utility.hexToBytes(value));
    }

    public static ByteString bytesToByteString(byte[] value) {
        return ByteString.copyFrom(value);
    }

    public static String byteStringToHex(ByteString value) {
        return Utility.bytesToHex(value.toByteArray());
    }
}
