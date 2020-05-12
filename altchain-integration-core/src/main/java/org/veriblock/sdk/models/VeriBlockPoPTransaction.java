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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VeriBlockPoPTransaction {
    private final Sha256Hash id;
    private final Address address;
    private final VeriBlockBlock publishedBlock;
    private final BitcoinTransaction bitcoinTransaction;
    private final MerklePath merklePath;
    private final BitcoinBlock blockOfProof;
    private final List<BitcoinBlock> blockOfProofContext;
    private final byte[] signature;
    private final byte[] publicKey;
    private final Byte networkByte;

    public Sha256Hash getId() {
        return id;
    }

    public Address getAddress() {
        return address;
    }

    public VeriBlockBlock getPublishedBlock() {
        return publishedBlock;
    }

    public BitcoinTransaction getBitcoinTransaction() {
        return bitcoinTransaction;
    }

    public MerklePath getMerklePath() {
        return merklePath;
    }

    public BitcoinBlock getBlockOfProof() {
        return blockOfProof;
    }

    public List<BitcoinBlock> getBlockOfProofContext() {
        return blockOfProofContext;
    }

    public List<BitcoinBlock> getBlocks() {
        List<BitcoinBlock> blocks = new ArrayList<>();
        if (blockOfProofContext != null) {
            blocks.addAll(blockOfProofContext);
        }

        if (blockOfProof != null) {
            blocks.add(blockOfProof);
        }

        return blocks;
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

    public VeriBlockPoPTransaction(Address address,
                                   VeriBlockBlock publishedBlock,
                                   BitcoinTransaction bitcoinTransaction,
                                   MerklePath merklePath,
                                   BitcoinBlock blockOfProof,
                                   List<BitcoinBlock> blockOfProofContext,
                                   byte[] signature,
                                   byte[] publicKey,
                                   Byte networkByte) {
        Preconditions.notNull(address, "Address cannot be null");
        Preconditions.notNull(publishedBlock, "PublishedBlock cannot be null");
        Preconditions.notNull(bitcoinTransaction, "BitcoinTransaction cannot be null");
        Preconditions.notNull(merklePath, "MerklePath cannot be null");
        Preconditions.notNull(blockOfProof, "BlockOfProof cannot be null");
        Preconditions.argument(signature != null && signature.length > 0, "Signature cannot be empty");
        Preconditions.argument(publicKey != null && publicKey.length > 0, "Public key cannot be empty");

        this.address = address;
        this.publishedBlock = publishedBlock;
        this.bitcoinTransaction = bitcoinTransaction;
        this.merklePath = merklePath;
        this.blockOfProof = blockOfProof;
        this.blockOfProofContext = blockOfProofContext != null ? blockOfProofContext : Collections.emptyList();
        this.signature = signature;
        this.publicKey = publicKey;
        this.networkByte = networkByte;

        this.id = SerializeDeserializeService.getId(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        VeriBlockPoPTransaction obj = (VeriBlockPoPTransaction)o;

        return SerializeDeserializeService.getId(this).equals(SerializeDeserializeService.getId(obj)) &&
                Arrays.equals(publicKey, obj.publicKey) &&
                Arrays.equals(signature, obj.signature);
    }
}
