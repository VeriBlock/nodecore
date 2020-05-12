// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

import org.veriblock.sdk.services.SerializeDeserializeService;
import org.veriblock.sdk.util.Preconditions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VeriBlockTransaction {
    
    private Sha256Hash id;
    private final byte type;
    private final Address sourceAddress;
    private final Coin sourceAmount;
    private final List<Output> outputs;
    private final long signatureIndex;
    private PublicationData publicationData;
    private final byte[] signature;
    private final byte[] publicKey;

    private final Byte networkByte;

    public Sha256Hash getId() {
        return this.id;
    }

    public void setId(Sha256Hash id) {
        this.id = id;
    }

    public byte getType() {
        return type;
    }

    public Address getSourceAddress() {
        return sourceAddress;
    }

    public Coin getSourceAmount() {
        return sourceAmount;
    }

    public List<Output> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    public long getSignatureIndex() {
        return signatureIndex;
    }

    public PublicationData getPublicationData() {
        return publicationData;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public Byte getNetworkByte() {
        return networkByte;
    }

    public VeriBlockTransaction(byte type,
                                Address sourceAddress,
                                Coin sourceAmount,
                                List<Output> outputs,
                                long signatureIndex,
                                PublicationData publicationData,
                                byte[] signature,
                                byte[] publicKey,
                                Byte networkByte) {
        Preconditions.notNull(sourceAddress, "Source address cannot be null");
        Preconditions.notNull(sourceAmount, "Source amount cannot be null");
        Preconditions.argument(signatureIndex >= 0, "Signature index must be positive");
        Preconditions.argument(signature != null && signature.length > 0, "Signature cannot be empty");
        Preconditions.argument(publicKey != null && publicKey.length > 0, "Public key cannot be empty");

        this.type = type;
        this.sourceAddress = sourceAddress;
        this.sourceAmount = sourceAmount;
        this.outputs = outputs != null ? outputs : Collections.emptyList();
        this.signatureIndex = signatureIndex;
        this.publicationData = publicationData;
        this.signature = signature;
        this.publicKey = publicKey;
        this.networkByte = networkByte;
        this.id = SerializeDeserializeService.getId(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        VeriBlockTransaction obj = (VeriBlockTransaction)o;

        return SerializeDeserializeService.getId(this).equals(SerializeDeserializeService.getId(obj)) &&
                Arrays.equals(publicKey, obj.publicKey) &&
                Arrays.equals(signature, obj.signature);
    }
}
