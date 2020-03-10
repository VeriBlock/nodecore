// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package veriblock.service.impl;

import org.veriblock.core.contracts.AddressManager;
import org.veriblock.core.crypto.Crypto;
import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.core.utilities.Utility;
import org.veriblock.sdk.models.Sha256Hash;
import veriblock.conf.NetworkParameters;
import veriblock.model.Output;
import veriblock.model.SigningResult;
import veriblock.model.StandardTransaction;
import veriblock.model.Transaction;

import java.security.PublicKey;
import java.util.List;

public class TransactionService {

    private final AddressManager addressManager;

    public TransactionService(AddressManager addressManager) {
        this.addressManager = addressManager;
    }

    /**
     * Create a standard signed transaction.
     *
     * @param inputAddress   The address tokens are being spent from
     * @param inputAmount    The quantity of tokens being removed from the input address
     * @param outputs        All _outputs (with their corresponding share of the sent tokens)
     * @param signatureIndex The index of signature for inputAddress
     * @return A StandardTransaction object
     */
    public Transaction createStandardTransaction(
        String inputAddress, Long inputAmount, List<Output> outputs, Long signatureIndex, NetworkParameters networkParameters
    ) {
        if (inputAddress == null) {
            throw new IllegalArgumentException("createStandardTransaction cannot be called with a null inputAddress!");
        }

        if (!AddressUtility.isValidStandardAddress(inputAddress)) {
            throw new IllegalArgumentException(
                "createStandardTransaction cannot be called with an invalid " + "inputAddress (" + inputAddress + ")!");
        }

        if (!Utility.isPositive(inputAmount)) {
            throw new IllegalArgumentException("createStandardTransaction cannot be called with a non-positive" +
                    "inputAmount (" + inputAmount + ")!");
        }

        long outputTotal = 0L;
        for (int outputCount = 0; outputCount < outputs.size(); outputCount++) {
            Output output = outputs.get(outputCount);

            if (output == null) {
                throw new IllegalArgumentException("createStandardTransaction cannot be called with a null output " +
                        "(at index " + outputCount + ")!");
            }

            Long outputAmount = output.getAmount().getAtomicUnits();
            if (!Utility.isPositive(outputAmount)) {
                throw new IllegalArgumentException("createStandardTransaction cannot be called with an output " +
                        "(at index " + outputCount + ") with a non-positive output amount!");
            }

            outputTotal += outputAmount;
        }

        if (outputTotal > inputAmount) {
            throw new IllegalArgumentException("createStandardTransaction cannot be called with an output total " +
                    "which is larger than the inputAmount (outputTotal = " + outputTotal + ", inputAmount = " +
                    inputAmount + ")!");
        }

        Transaction transaction = new StandardTransaction(inputAddress, inputAmount, outputs, signatureIndex, networkParameters);

        SigningResult signingResult = signTransaction(transaction.getTxId(), inputAddress);

        if (signingResult.succeeded()) {
            transaction.addSignature(signingResult.getSignature(), signingResult.getPublicKey());
        }

        return transaction;
    }

    public int predictStandardTransactionToAllStandardOutputSize(long inputAmount, List<Output> outputs, long sigIndex, int extraDataLength) {
        int totalSize = 0;
        totalSize += 1; // Transaction Version
        totalSize += 1; // Type of Input Address
        totalSize += 1; // Standard Input Address Length Byte
        totalSize += 22; // Standard Input Address Length

        byte[] inputAmountBytes = Utility.trimmedByteArrayFromLong(inputAmount);

        totalSize += 1; // Input Amount Length Byte
        totalSize += inputAmountBytes.length; // Input Amount Length

        totalSize += 1; // Number of Outputs

        for (int i = 0; i < outputs.size(); i++) {
            totalSize += 1; // ID of Output Address
            totalSize += 1; // Output Address Length Bytes
            totalSize += 22; // Output Address Length

            byte[] outputAmount = Utility.trimmedByteArrayFromLong(outputs.get(i).getAmount().getAtomicUnits());
            totalSize += 1; // Output Amount Length Bytes
            totalSize += outputAmount.length; // Output Amount Length
        }

        byte[] sigIndexBytes = Utility.trimmedByteArrayFromLong(sigIndex);
        totalSize += 1; // Sig Index Length Bytes
        totalSize += sigIndexBytes.length; // Sig Index Bytes

        byte[] dataLengthBytes = Utility.trimmedByteArrayFromInteger(extraDataLength);
        totalSize += 1; // Data Length Bytes Length
        totalSize += dataLengthBytes.length; // Data Length Bytes
        totalSize += extraDataLength; // Extra data section

        return totalSize;
    }

    public static Sha256Hash calculateTxID(Transaction transaction, NetworkParameters networkParameters) {
        return Sha256Hash.wrap(calculateTxIDBytes(transaction.toByteArray(networkParameters)));
    }

    public static byte[] calculateTxIDBytes(byte[] rawTx) {
        return new Crypto().SHA256ReturnBytes(rawTx);
    }

    public SigningResult signTransaction(Sha256Hash txId, String address) {
        byte[] signature = addressManager.signMessage(txId.getBytes(), address);
        if (signature == null) {
            return new SigningResult(false, null, null);
        }

        PublicKey publicKey = addressManager.getPublicKeyForAddress(address);
        if (publicKey == null) {
            return new SigningResult(false, null, null);
        }

        return new SigningResult(true, signature, publicKey.getEncoded());
    }

}
