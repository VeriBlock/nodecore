// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities;

import org.veriblock.core.contracts.Output;
import org.veriblock.core.contracts.TransactionAddress;
import org.veriblock.core.contracts.TransactionAmount;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Stream;

public class SerializerUtility {
    // returns new array of size at least 'size'. if val.len < size, prepends zeroes
    public static byte[] pad(byte[] val, int size) {
        checkRange(size, 1, 8, "size can be 1<=size<=8");
        if (val.length >= size) {
            return val;
        }

        int zeroes = size - val.length;

        byte[] ret = new byte[size];
        System.arraycopy(ret, 0, ret, zeroes, val.length);
        return ret;
    }

    public static void checkRange(int length, int min, int max, String msg) {
        if (length > max) {
            throw new IllegalArgumentException(String.format("Value %d is bigger than maximum %d. Details: %s.", length, max, msg));
        }

        if (length < min) {
            throw new IllegalArgumentException(String.format("Value %d is smaller than minimum %d. Details: %s.", length, min, msg));
        }
    }

    public static byte[] readVariableLengthValueFromStream(ByteBuffer stream, int minLen, int maxLen) {
        int length = Math.toIntExact(readSingleBEValue(stream, 4));
        checkRange(length, minLen, maxLen, "Can not read varLenValue of size 4");
        byte[] buf = new byte[length];
        stream.get(buf);
        return buf;
    }

    public static Long readSingleBEValue(ByteBuffer stream, int size) {
        checkRange(size, 1, 8, "Size can be 1<=size<=8");
        byte[] buf = readSingleByteLenValue(stream, 0, size);
        buf = pad(buf, size);
        ByteBuffer buffer = ByteBuffer.wrap(buf);
        if (size == 8) {
            return buffer.getLong();
        }
        if (size == 4) {
            return (long) buffer.getInt();
        }
        if (size == 2) {
            return (long) buffer.getShort();
        }
        if (size == 1) {
            return (long) buffer.get();
        }

        throw new IllegalArgumentException(String.format("Invalid size=%d, can be 1,2,4,8", size));
    }

    public static byte[] readSingleByteLenValue(ByteBuffer stream, int minLen, int maxLen) {
        byte len = stream.get();
        checkRange(len, minLen, maxLen, "Can not read single byte len value");

        byte[] buf = new byte[len];
        stream.get(buf);
        return buf;
    }

    public static void writeVariableLengthValueToStream(OutputStream stream, int value) throws IOException {
        byte[] trimmed = Utility.trimmedByteArrayFromInteger(value);
        stream.write(trimmed.length);
        stream.write(trimmed);
    }

    public static void writeVariableLengthValueToStream(OutputStream stream, long value) throws IOException {
        byte[] trimmed = Utility.trimmedByteArrayFromLong(value);
        stream.write(trimmed.length);
        stream.write(trimmed);
    }

    public static void writeVariableLengthValueToStream(OutputStream stream, byte[] value) throws IOException {
        byte[] dataSize = Utility.trimmedByteArrayFromInteger(value.length);
        stream.write((byte) dataSize.length);
        stream.write(dataSize);
        stream.write(value);
    }

    public static void writeSingleByteLengthValueToStream(OutputStream stream, byte[] value) throws IOException {
        stream.write((byte) value.length);
        stream.write(value);
    }

    public static void serializeRegularTransactionToStream(
        OutputStream stream,
        Byte magicByte,
        byte transactionType,
        TransactionAddress sourceAddress,
        TransactionAmount sourceAmount,
        List<Output> outputs,
        long signatureIndex,
        byte[] data
    ) throws IOException {
        // Magic byte is used on non-mainnet networks as replay protection
        if (magicByte != null) {
            stream.write(magicByte);
        }

        // Write type
        stream.write(transactionType);

        // Write source address
        sourceAddress.serializeToStream(stream);

        // Write source amount
        sourceAmount.serializeToStream(stream);

        // Write destinations
        stream.write((byte) outputs.size());
        for (Output _output : outputs) {
            _output.serializeToStream(stream);
        }

        SerializerUtility.writeVariableLengthValueToStream(stream, signatureIndex);
        SerializerUtility.writeVariableLengthValueToStream(stream, data);
    }
}
