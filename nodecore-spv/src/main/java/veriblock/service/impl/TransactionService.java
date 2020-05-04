// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package veriblock.service.impl;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.core.contracts.AddressManager;
import org.veriblock.core.crypto.Crypto;
import org.veriblock.core.types.Pair;
import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.core.utilities.Utility;
import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.models.Sha256Hash;
import veriblock.conf.NetworkParameters;
import veriblock.model.AddressCoinsIndex;
import veriblock.model.Output;
import veriblock.model.SigningResult;
import veriblock.model.StandardTransaction;
import veriblock.model.Transaction;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionService {

    private static final long DEFAULT_TRANSACTION_FEE = 1000L;
    private final AddressManager addressManager;
    private final NetworkParameters networkParameters;

    public TransactionService(AddressManager addressManager, NetworkParameters networkParameters) {
        this.addressManager = addressManager;
        this.networkParameters = networkParameters;
    }

    public long calculateFee(String requestedSourceAddress, long totalOutputAmount, List<Output> outputList, long signatureIndex) {
        // This is for over-estimating the size of the transaction by one byte in the edge case where totalOutputAmount
        // is right below a power-of-two barrier
        long feeFudgeFactor = DEFAULT_TRANSACTION_FEE * 500L;

        int predictedTransactionSize =
            predictStandardTransactionToAllStandardOutputSize(totalOutputAmount + feeFudgeFactor, outputList, signatureIndex + 1, 0);

        return predictedTransactionSize * DEFAULT_TRANSACTION_FEE;
    }

    public List<Transaction> createTransactionsByOutputList(List<AddressCoinsIndex> addressCoinsIndexList, List<Output> outputList) {
        List<Transaction> transactions = new ArrayList<>();
        List<Output> sortedOutputs = outputList.stream()
            .sorted((o1, o2) -> Long.compare(o2.getAmount().getAtomicUnits(), o1.getAmount().getAtomicUnits()))
            .collect(Collectors.toList());
        List<AddressCoinsIndex> sortedAddressCoinsIndexList = addressCoinsIndexList.stream()
            .filter(b -> b.getCoins() > 0)
            .sorted((b1, b2) -> Long.compare(b2.getCoins(), b1.getCoins()))
            .collect(Collectors.toList());

        long totalOutputAmount = sortedOutputs.stream()
            .map(output -> output.getAmount().getAtomicUnits())
            .reduce(0L, Long::sum);

        for (AddressCoinsIndex sourceAddressesIndex : sortedAddressCoinsIndexList) {
            long fee = calculateFee(sourceAddressesIndex.getAddress(), totalOutputAmount, sortedOutputs, sourceAddressesIndex.getIndex());

            Pair<List<Output>, List<Output>> fulfillAndForPay =
                splitOutPutsAccordingBalance(sortedOutputs, Coin.valueOf(sourceAddressesIndex.getCoins() - fee));
            sortedOutputs = fulfillAndForPay.getFirst();
            List<Output> outputsForPay = fulfillAndForPay.getSecond();

            long transactionInputAmount = outputsForPay.stream()
                .map(o -> o.getAmount().getAtomicUnits())
                .reduce(0L, Long::sum) + fee;

            Transaction transaction =
                createStandardTransaction(
                    sourceAddressesIndex.getAddress(), transactionInputAmount, outputsForPay, sourceAddressesIndex.getIndex() + 1);
            transactions.add(transaction);
            if (sortedOutputs.size() == 0) {
                break;
            }
        }
        return transactions;
    }

    public Pair<List<Output>, List<Output>> splitOutPutsAccordingBalance(List<Output> outputs, Coin balance) {
        List<Output> outputsLeft = new ArrayList<>();
        List<Output> outputsForPay = new ArrayList<>();
        Coin balanceLeft = balance;

        for (int i = 0; i < outputs.size(); i++) {
            Output output = outputs.get(i);

            if (balanceLeft.getAtomicUnits() > output.getAmount().getAtomicUnits()) {
                outputsForPay.add(output);
                balanceLeft = balance.subtract(output.getAmount());
            } else {
                Output partForPay = new Output(output.getAddress(), balanceLeft);
                outputsForPay.add(partForPay);

                Coin leftToPay = output.getAmount().subtract(balanceLeft);
                outputs.add(new Output(output.getAddress(), leftToPay));
                outputs.remove(output);

                for (int j = i; j < outputs.size(); j++) {
                    outputsLeft.add(outputs.get(j));
                }

                return new Pair<>(outputsLeft, outputsForPay);
            }
        }
        return new Pair<>(outputsLeft, outputsForPay);
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
    public Transaction createStandardTransaction(String inputAddress, Long inputAmount, List<Output> outputs, Long signatureIndex) {
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

    public Transaction createUnsignedAltChainEndorsementTransaction(
        String inputAddress, long fee, byte[] publicationData, long signatureIndex
    ) {
        if (inputAddress == null) {
            throw new IllegalArgumentException("createAltChainEndorsementTransaction cannot be called with a null inputAddress!");
        }

        if (!AddressUtility.isValidStandardAddress(inputAddress)) {
            throw new IllegalArgumentException(
                "createAltChainEndorsementTransaction cannot be called with an invalid " + "inputAddress (" + inputAddress + ")!");
        }

        if (!Utility.isPositive(fee)) {
            throw new IllegalArgumentException(
                "createAltChainEndorsementTransaction cannot be called with a non-positive" + "inputAmount (" + fee + ")!");
        }

        return new StandardTransaction(null, inputAddress, fee, Collections.emptyList(), signatureIndex, publicationData, networkParameters);
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

    public static int predictAltChainEndorsementTransactionSize(int dataLength, long sigIndex) {
        int totalSize = 0;

        // Using an estimated total fee of 1 VBK
        long inputAmount = 100000000L;
        long inputAmountLength = 0L;

        totalSize += 1; // Transaction Version
        totalSize += 1; // Type of Input Address
        totalSize += 1; // Standard Input Address Length Byte
        totalSize += 22; // Standard Input Address Length

        byte[] inputAmountBytes = Utility.trimmedByteArrayFromLong(inputAmount);
        inputAmountLength = inputAmountBytes.length;

        totalSize += 1; // Input Amount Length Byte
        totalSize += inputAmountLength; // Input Amount Length

        totalSize += 1; // Number of Outputs, will be 0

        byte[] sigIndexBytes = Utility.trimmedByteArrayFromLong(sigIndex);
        totalSize += 1; // Sig Index Length Bytes
        totalSize += sigIndexBytes.length; // Sig Index Bytes

        byte[] dataSizeBytes = Utility.trimmedByteArrayFromInteger(dataLength);
        totalSize += 1; // Data Length Bytes Length
        totalSize += dataSizeBytes.length; // Data Length Bytes (value will be 0)
        totalSize += dataLength;

        return totalSize;
    }

    public static VeriBlockMessages.Transaction.Builder getRegularTransactionMessageBuilder(StandardTransaction tx) {
        VeriBlockMessages.Transaction.Builder builder = VeriBlockMessages.Transaction.newBuilder();
        builder.setTransactionFee(tx.getTransactionFee());
        builder.setTxId(ByteString.copyFrom(tx.getTxId().getBytes()));
        builder.setType(VeriBlockMessages.Transaction.Type.STANDARD);
        builder.setSourceAmount(tx.getInputAmount().getAtomicUnits());
        builder.setSourceAddress(ByteString.copyFrom(tx.getInputAddress().toByteArray()));
        builder.setData(ByteString.copyFrom(tx.getData()));
        //        builder.setTimestamp(getTimeStamp());
        //        builder.setSize(tx.getSize());
        for (Output output : tx.getOutputs()) {
            VeriBlockMessages.Output.Builder outputBuilder = builder.addOutputsBuilder();
            outputBuilder.setAddress(ByteString.copyFrom(output.getAddress().toByteArray()));
            outputBuilder.setAmount(output.getAmount().getAtomicUnits());
        }
        return builder;
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
