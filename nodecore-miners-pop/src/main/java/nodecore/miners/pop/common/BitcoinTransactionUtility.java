// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

public class BitcoinTransactionUtility {
    private static final Logger _logger = LoggerFactory.getLogger(BitcoinTransactionUtility.class);

    /**
     * Returns a String representing the DefaultRpcAgent-order txid for the given hexadecimal bitcoin DefaultTransaction data
     *
     * @param bitcoinTransactionDataForTxID A byte array containing a Bitcoin transaction
     * @return The hexadecimal, DefaultRpcAgent-order transaction ID
     */
    public static String getTransactionId(byte[] bitcoinTransactionDataForTxID) {
        if (bitcoinTransactionDataForTxID == null) {
            throw new IllegalArgumentException("getTransactionId cannot be called with a null Bitcoin Transaction byte array!");
        }

        Crypto c = new Crypto();
        return Utility.bytesToHex(Utility.flip(c.SHA256D(bitcoinTransactionDataForTxID)));
    }

    public static byte[] parseTxIDRelevantBits(byte[] fullBitcoinTransaction) {
        try {
            byte[] possibleSegwitFlag = new byte[2];
            possibleSegwitFlag[0] = fullBitcoinTransaction[4];
            possibleSegwitFlag[1] = fullBitcoinTransaction[5];

            if (!(possibleSegwitFlag[0] == 0x00 && possibleSegwitFlag[1] == 0x01)) {
                // Transaction is not segwit, so it is already in the correct serialization for TxID calculation
                // Unless another type of transaction serialization is invented
                return fullBitcoinTransaction;
            }

            byte[] version = new byte[4];
            System.arraycopy(fullBitcoinTransaction, 0, version, 0, 4);

            int cursor = 6; // 4 bytes for version, 2 bytes for segwit flag

            byte[] numInputsVInt = grabVInt(fullBitcoinTransaction, cursor);

            cursor += numInputsVInt.length;

            long numInputs = parseVInt(numInputsVInt);

            if (numInputs <= 0 || numInputs > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "This decoder does not support transactions with an input count outside of the range [1, Integer.MAX_VALUE]!");
            }

            byte[][] serializedInputs = new byte[(int) numInputs][];

            // Read all of the inputs
            for (int i = 0; i < numInputs; i++) {
                // Each input references an input TxID which is 32 bytes
                byte[] inputTxID = new byte[32];
                System.arraycopy(fullBitcoinTransaction, cursor, inputTxID, 0, inputTxID.length);
                cursor += inputTxID.length;

                // Each input references the index of the consumed output of the referenced transaction, 4 bytes
                byte[] vInIndex = new byte[4];
                System.arraycopy(fullBitcoinTransaction, cursor, vInIndex, 0, vInIndex.length);
                cursor += vInIndex.length;

                byte[] scriptLengthVInt = grabVInt(fullBitcoinTransaction, cursor);
                cursor += scriptLengthVInt.length;

                long scriptLength = parseVInt(scriptLengthVInt);

                if (scriptLength <= 0 || scriptLength > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException(
                            "This decoder does not support inputs with script lengths outside the range [1, Integer.MAX_VALUE]!");
                }

                byte[] script = new byte[(int) scriptLength];
                System.arraycopy(fullBitcoinTransaction, cursor, script, 0, script.length);
                cursor += script.length;

                // Sequence is always 4 bytes, often 0xFFFFFFFF
                byte[] sequence = new byte[4];
                System.arraycopy(fullBitcoinTransaction, cursor, sequence, 0, sequence.length);
                cursor += sequence.length;

                byte[] serializedInput = new byte[inputTxID.length + vInIndex.length + scriptLengthVInt.length + script.length + sequence.length];

                int subCursor = 0;
                System.arraycopy(inputTxID, 0, serializedInput, subCursor, inputTxID.length);
                subCursor += inputTxID.length;
                System.arraycopy(vInIndex, 0, serializedInput, subCursor, vInIndex.length);
                subCursor += vInIndex.length;
                System.arraycopy(scriptLengthVInt, 0, serializedInput, subCursor, scriptLengthVInt.length);
                subCursor += scriptLengthVInt.length;
                System.arraycopy(script, 0, serializedInput, subCursor, script.length);
                subCursor += script.length;
                System.arraycopy(sequence, 0, serializedInput, subCursor, sequence.length);

                serializedInputs[i] = serializedInput;
            }

            byte[] numOutputsVInt = grabVInt(fullBitcoinTransaction, cursor);

            cursor += numOutputsVInt.length;

            long numOutputs = parseVInt(numOutputsVInt);

            if (numInputs <= 0 || numInputs > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "This decoder does not support transactions with an output count outside of the range [1, Integer.MAX_VALUE]!");
            }

            // Read all the outputs
            byte[][] serializedOutputs = new byte[(int) numOutputs][];
            for (int i = 0; i < numOutputs; i++) {
                // Satoshis in output always encoded in 64-bit int
                byte[] satoshisInOutput = new byte[8];
                System.arraycopy(fullBitcoinTransaction, cursor, satoshisInOutput, 0, satoshisInOutput.length);
                cursor += satoshisInOutput.length;

                byte[] scriptLengthVInt = grabVInt(fullBitcoinTransaction, cursor);
                cursor += scriptLengthVInt.length;

                long scriptLength = parseVInt(scriptLengthVInt);

                if (scriptLength <= 0 || scriptLength > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException(
                            "This decoder does not support outputs with script lengths outside the range [1, Integer.MAX_VALUE]!");
                }

                byte[] script = new byte[(int) scriptLength];
                System.arraycopy(fullBitcoinTransaction, cursor, script, 0, script.length);
                cursor += script.length;

                byte[] serializedOutput = new byte[satoshisInOutput.length + scriptLengthVInt.length + script.length];

                int subCursor = 0;
                System.arraycopy(satoshisInOutput, 0, serializedOutput, subCursor, satoshisInOutput.length);
                subCursor += satoshisInOutput.length;
                System.arraycopy(scriptLengthVInt, 0, serializedOutput, subCursor, scriptLengthVInt.length);
                subCursor += scriptLengthVInt.length;
                System.arraycopy(script, 0, serializedOutput, subCursor, script.length);

                serializedOutputs[i] = serializedOutput;
            }

            // Locktime is the last 4 bytes of the transaction
            byte[] locktime = new byte[4];
            System.arraycopy(fullBitcoinTransaction, fullBitcoinTransaction.length - locktime.length, locktime, 0, locktime.length);

            ByteArrayOutputStream transactionForTxIDCalculation = new ByteArrayOutputStream();
            transactionForTxIDCalculation.write(version);
            transactionForTxIDCalculation.write(numInputsVInt);
            for (int i = 0; i < serializedInputs.length; i++) {
                transactionForTxIDCalculation.write(serializedInputs[i]);
            }
            transactionForTxIDCalculation.write(numOutputsVInt);
            for (int i = 0; i < serializedOutputs.length; i++) {
                transactionForTxIDCalculation.write(serializedOutputs[i]);
            }
            transactionForTxIDCalculation.write(locktime);
            return transactionForTxIDCalculation.toByteArray();
        } catch (Exception e) {
            _logger.info("A Bitcoin transaction (" + Utility.bytesToHex(fullBitcoinTransaction) + ") could not be parsed!", e);
            return null;
        }
    }

    private static long parseVInt(byte[] vInt) {
        if (vInt == null) {
            throw new IllegalArgumentException("parseVInt cannot be called with a null vInt!");
        }
        if (vInt.length != 1 && vInt.length != 3 && vInt.length != 5 && vInt.length != 9) {
            throw new IllegalArgumentException("parseVInt cannot be called with a vInt of length (in bytes) 1, 3, 5, or 9!");
        }

        if (vInt[0] == (byte) 0xFF) {
            if (vInt.length != 9) {
                throw new IllegalArgumentException("parseVInt cannot be called with a vInt with a starting byte 0xFF and a length that isn't 9!");
            }

            byte[] numberPart = new byte[8];
            System.arraycopy(vInt, 1, numberPart, 0, numberPart.length);

            long result = 0L;
            result += ((long) numberPart[0]);
            result += ((long) numberPart[1] << 8);
            result += ((long) numberPart[2] << 16);
            result += ((long) numberPart[3] << 24);
            result += ((long) numberPart[4] << 32);
            result += ((long) numberPart[5] << 40);
            result += ((long) numberPart[6] << 48);
            result += ((long) numberPart[7] << 56);

            return result;
        } else if (vInt[0] == (byte) 0xFE) {
            if (vInt.length != 5) {
                throw new IllegalArgumentException("parseVInt cannot be called with a vInt with a starting byte 0xFE and a length that isn't 5!");
            }

            byte[] numberPart = new byte[4];
            System.arraycopy(vInt, 1, numberPart, 0, numberPart.length);

            long result = 0L;
            result += ((long) numberPart[0]);
            result += ((long) numberPart[1] << 8);
            result += ((long) numberPart[2] << 16);
            result += ((long) numberPart[3] << 24);

            return result;
        } else if (vInt[0] == (byte) 0xFD) {
            if (vInt.length != 3) {
                throw new IllegalArgumentException("parseVInt cannot be called with a vInt with a starting byte 0xFE and a length that isn't 3!");
            }

            byte[] numberPart = new byte[2];
            System.arraycopy(vInt, 1, numberPart, 0, numberPart.length);

            long result = 0L;
            result += ((long) numberPart[0]);
            result += ((long) numberPart[1] << 8);

            return result;
        } else {
            return (long) vInt[0];
        }
    }

    private static byte[] grabVInt(byte[] bitcoinTransaction, int offset) {
        byte start = bitcoinTransaction[offset];

        int vIntLength;

        if (start == (byte) 0xFF) {
            vIntLength = 9;
        } else if (start == (byte) 0xFE) {
            vIntLength = 5;
        } else if (start == (byte) 0xFD) {
            vIntLength = 3;
        } else {
            vIntLength = 1;
        }

        byte[] result = new byte[vIntLength];
        System.arraycopy(bitcoinTransaction, offset, result, 0, vIntLength);
        return result;
    }

    /**
     * Determines whether the raw Bitcoin transaction contains the specified data.
     * Generally, this will find the data stashed in OP_RETURN, but does not explicitly seek out OP_RETURNS to future-proof for
     * other methods of directly embedding bytes into a bitcoin transaction.
     *
     * @param bitcoinTransaction The bitcoin transaction to search through
     * @param data               The data to check for in the bitcoin transaction
     * @return Whether or not the provided data appears in the provided bitcoin transaction
     */
    public static boolean transactionContainsData(byte[] bitcoinTransaction, byte[] data) {
        if (bitcoinTransaction == null) {
            throw new IllegalArgumentException("transactionContainsData cannot be called with a null bitcoinTransaction byte array!");
        }

        if (data == null) {
            throw new IllegalArgumentException("transactionContainsData cannot be called with a null data byte array!");
        }

        for (int i = 0; i < bitcoinTransaction.length - data.length; i++) {
            int j;
            for (j = 0; j < data.length; j++) {
                if (bitcoinTransaction[i + j] != data[j]) {
                    break;
                }
            }

            if (j == data.length) {
                return true;
            }
        }
        return false;
    }
}
