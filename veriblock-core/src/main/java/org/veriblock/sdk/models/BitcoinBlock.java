// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

import org.veriblock.core.crypto.Sha256Hash;
import org.veriblock.sdk.services.SerializeDeserializeService;

import java.util.Arrays;

public class BitcoinBlock {
    private final byte[] raw;
    private final int version;
    private final Sha256Hash previousBlock;
    private final Sha256Hash merkleRoot;
    private final int timestamp;
    private final int bits;
    private final int nonce;
    private final Sha256Hash hash;

    public BitcoinBlock(int version, Sha256Hash previousBlock, Sha256Hash merkleRoot, int timestamp, int bits, int nonce) {
        this.version = version;
        this.previousBlock = previousBlock;
        this.merkleRoot = merkleRoot;
        this.timestamp = timestamp;
        this.bits = bits;
        this.nonce = nonce;
        //TODO move it out of here.
        this.raw = SerializeDeserializeService.getHeaderBytesBitcoinBlock(this);
        this.hash = Sha256Hash.wrapReversed(Sha256Hash.hashTwice(this.raw));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass() && Arrays.equals(SerializeDeserializeService.getHeaderBytesBitcoinBlock(this), SerializeDeserializeService
            .getHeaderBytesBitcoinBlock((BitcoinBlock) o));
    }

    public byte[] getRaw() {
        return raw;
    }

    public int getVersion() {
        return version;
    }

    public Sha256Hash getPreviousBlock() {
        return previousBlock;
    }

    public Sha256Hash getMerkleRoot() {
        return merkleRoot;
    }

    public Sha256Hash getMerkleRootReversed() {
        return Sha256Hash.wrap(merkleRoot.getReversedBytes());
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getBits() {
        return bits;
    }

    public int getNonce() {
        return nonce;
    }

    public Sha256Hash getHash() {
        return this.hash;
    }
}
