// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import nodecore.api.grpc.VeriBlockMessages;
import org.veriblock.sdk.models.Sha256Hash;
import veriblock.conf.NetworkParameters;

import java.util.List;


public abstract class Transaction {
    protected Sha256Hash txId;
    protected AddressLight inputAddress;
    protected TransactionMeta transactionMeta;

    private byte[] signature;
    private byte[] publicKey;

    public Transaction(Sha256Hash txId) {
        this.txId = txId;
        this.transactionMeta = new TransactionMeta(txId);
    }

    public Transaction() {}

    public abstract List<Output> getOutputs();

    public abstract long getSignatureIndex();

    public abstract void setSignatureIndex(long index);

    public abstract byte[] getData();

    public abstract long getTransactionFee();

    public abstract TransactionTypeIdentifier getTransactionTypeIdentifier();

    public abstract byte[] toByteArray(NetworkParameters networkParameters);

    public abstract VeriBlockMessages.SignedTransaction.Builder getSignedMessageBuilder(NetworkParameters networkParameters);

    public final Sha256Hash getTxId() {
        return txId;
    }

    public void setTxId(Sha256Hash txId) {
        this.txId = txId;
    }

    public final TransactionMeta getTransactionMeta() {
        return transactionMeta;
    }
    public final void setTransactionMeta(TransactionMeta transactionMeta) {
        this.transactionMeta = transactionMeta;
    }

    public final AddressLight getInputAddress() {
        return inputAddress;
    }
    public final void setInputAddress(AddressLight inputAddress) {
        this.inputAddress = inputAddress;
    }

    public void addSignature(byte[] signed, byte[] publicKey){
        setSignature(signed);
        setPublicKey(publicKey);
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

}
