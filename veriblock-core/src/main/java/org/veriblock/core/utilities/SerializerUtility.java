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
import java.io.OutputStream;
import java.util.List;

public class SerializerUtility {
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
        stream.write((byte)dataSize.length);
        stream.write(dataSize);
        stream.write(value);
    }

    public static void writeSingleByteLengthValueToStream(OutputStream stream, byte[] value) throws IOException {
        stream.write((byte)value.length);
        stream.write(value);
    }

    public static void serializeRegularTransactionToStream(OutputStream stream, Byte magicByte, byte transactionType, TransactionAddress sourceAddress, TransactionAmount sourceAmount, List<Output> outputs, long signatureIndex, byte[] data) throws IOException {
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
        stream.write((byte)outputs.size());
        for (Output _output : outputs) {
            _output.serializeToStream(stream);
        }

        SerializerUtility.writeVariableLengthValueToStream(stream, signatureIndex);
        SerializerUtility.writeVariableLengthValueToStream(stream, data);
    }
}
