// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.utilities;

import org.veriblock.core.DefaultOutput;
import org.veriblock.core.contracts.Output;
import org.veriblock.core.contracts.TransactionAddress;
import org.veriblock.core.contracts.TransactionAmount;
import org.veriblock.core.crypto.Crypto;
import org.veriblock.core.types.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class TransactionUtility {
    public static byte[] serializeTransactionEffects(byte type, Pair<String, Long> input,
                                                     List<Output> outputs, long signatureIndex) {
        ByteArrayOutputStream serializedData = new ByteArrayOutputStream();
        try {
            // Write type
            serializedData.write(type);

            new TransactionAddress(input.getFirst()).serializeToStream(serializedData);
            new TransactionAmount(input.getSecond()).serializeToStream(serializedData);

            // Write destinations
            serializedData.write((byte)outputs.size());
            for (int i = 0; i < outputs.size(); i++) {
                outputs.get(i).serializeToStream(serializedData);
            }

            SerializerUtility.writeVariableLengthValueToStream(serializedData, signatureIndex);
            SerializerUtility.writeVariableLengthValueToStream(serializedData, new byte[]{});

        } catch (IOException e) {
        }

        return serializedData.toByteArray();
    }

    public static byte[] calculateTxIDByteArray(byte type, Pair<String, Long> input, List<Output> outputs, long signatureIndex) {
        return new Crypto().SHA256ReturnBytes(serializeTransactionEffects(type, input, outputs, signatureIndex));
    }
}
