// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import com.google.protobuf.ByteString;
import nodecore.api.grpc.VeriBlockMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.SerializerUtility;
import org.veriblock.core.utilities.Utility;
import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.services.SerializeDeserializeService;
import veriblock.conf.MainNetParameters;
import veriblock.conf.NetworkParameters;
import veriblock.service.impl.TransactionService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static nodecore.api.grpc.utilities.ByteStringUtility.hexToByteString;

public class StandardTransaction extends Transaction {
    private static final Logger logger = LoggerFactory.getLogger(StandardTransaction.class);

    public static final byte NON_MAINNET_TRANSACTION_SERIALIZATION_PREPEND = (byte)0xAA;

    @Override
    public TransactionTypeIdentifier getTransactionTypeIdentifier() {
        return TransactionTypeIdentifier.STANDARD;
    }

    private Coin inputAmount;
    private final List<Output> outputs = new ArrayList<>();
    private long signatureIndex;
    private long transactionFee;
    private byte[] data;

    public StandardTransaction(Sha256Hash txId) {
        super(txId);
    }

    public StandardTransaction(
        String inputAddress, long inputAmount, List<Output> outputs, long signatureIndex, NetworkParameters networkParameters
    ) {
        this(null, inputAddress, inputAmount, outputs, signatureIndex, new byte[0], networkParameters);
    }

    public StandardTransaction(
        Sha256Hash txId,
        String inputAddress,
        long inputAmount,
        List<Output> outputs,
        long signatureIndex,
        byte[] data,
        NetworkParameters networkParameters
    ) {

        long totalOutput = 0L;
        for (Output o : outputs) {
            totalOutput += o.getAmount().getAtomicUnits();
        }

        long fee = inputAmount - totalOutput;

        // Only for Alt Chain Endorsement Transactions
        this.setData(data);
        this.setSignatureIndex(signatureIndex);
        this.addAllOutput(outputs);
        this.setInputAmount(Coin.valueOf(inputAmount));
        this.setInputAddress(new StandardAddress(inputAddress));
        this.setTransactionFee(fee);

        if (txId == null) {
            this.setTxId(TransactionService.calculateTxID(this, networkParameters));
        } else {
            this.setTxId(txId);
        }
    }

    public byte[] toByteArray(NetworkParameters networkParameters) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            serializeToStream(stream, networkParameters);

            return stream.toByteArray();
        } catch (IOException e) {
            // Should not happen
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    public VeriBlockMessages.SignedTransaction.Builder getSignedMessageBuilder(NetworkParameters networkParameters) {
        VeriBlockMessages.Transaction transaction = getTransactionMessageBuilder(networkParameters).build();
        VeriBlockMessages.SignedTransaction.Builder builder = VeriBlockMessages.SignedTransaction.newBuilder();
        builder.setTransaction(transaction);
        builder.setSignatureIndex(getSignatureIndex());
        builder.setPublicKey(ByteString.copyFrom(getPublicKey()));
        builder.setSignature(ByteString.copyFrom(getSignature()));
        return builder;
    }

    private VeriBlockMessages.Transaction.Builder getTransactionMessageBuilder(NetworkParameters networkParameters) {
        VeriBlockMessages.Transaction.Builder builder = VeriBlockMessages.Transaction.newBuilder();
        builder.setTimestamp(Utility.getCurrentTimeSeconds());
        builder.setTransactionFee(getTransactionFee());

        builder.setTxId(hexToByteString(getTxId().toString()));

        if (getTransactionTypeIdentifier() == TransactionTypeIdentifier.STANDARD) {
            builder.setType(VeriBlockMessages.Transaction.Type.STANDARD);
        } else if (getTransactionTypeIdentifier() == TransactionTypeIdentifier.MULTISIG) {
            builder.setType(VeriBlockMessages.Transaction.Type.MULTISIG);
        }

        builder.setSourceAmount(getInputAmount().getAtomicUnits());

        builder.setSourceAddress(ByteString.copyFrom(getInputAddress().toByteArray()));

        builder.setData(ByteString.copyFrom(getData()));

        builder.setSize(toByteArray(networkParameters).length);

        for (Output output : getOutputs()) {
            VeriBlockMessages.Output.Builder outputBuilder = builder.addOutputsBuilder();

            outputBuilder.setAddress(ByteString.copyFrom(output.getAddress().toByteArray()));
            outputBuilder.setAmount(output.getAmount().getAtomicUnits());
        }

        return builder;
    }

    private void serializeToStream(OutputStream stream, NetworkParameters networkParameters) throws IOException {
        Byte magicByte = !networkParameters.getNetworkName().equals(MainNetParameters.NETWORK) ? NON_MAINNET_TRANSACTION_SERIALIZATION_PREPEND : null;

        if (magicByte != null) {
            stream.write(magicByte);
        }

        // Write type
        stream.write(getTransactionTypeIdentifier().id());

        // Write source address
        getInputAddress().serializeToStream(stream);

        // Write source amount
        SerializeDeserializeService.serialize(getInputAmount(), stream);

        // Write destinations
        stream.write((byte) getOutputs().size());
        for (Output output : getOutputs()) {
            output.serializeToStream(stream);
        }

        SerializerUtility.writeVariableLengthValueToStream(stream, getSignatureIndex());
        SerializerUtility.writeVariableLengthValueToStream(stream, getData());
    }

    public Coin getInputAmount() {
        return inputAmount;
    }

    public void setInputAmount(Coin inputAmount) {
        this.inputAmount = inputAmount;
    }


    public List<Output> getOutputs() {
        return outputs;
    }

    public void addOutput(Output o) {
        outputs.add(o);
    }

    public void addAllOutput(Collection<? extends Output> o) {
        outputs.addAll(o);
    }


    public long getSignatureIndex() {
        return signatureIndex;
    }

    public void setSignatureIndex(long signatureIndex) {
        this.signatureIndex = signatureIndex;
    }


    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getTransactionFee() {
        return transactionFee;
    }

    public void setTransactionFee(long transactionFee) {
        this.transactionFee = transactionFee;
    }


}
