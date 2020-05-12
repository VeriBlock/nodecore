// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.blockchain.store;

import org.veriblock.sdk.models.Constants;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.models.VBlakeHash;
import org.veriblock.sdk.models.VeriBlockBlock;
import org.veriblock.sdk.services.SerializeDeserializeService;
import org.veriblock.sdk.util.Preconditions;
import org.veriblock.sdk.util.Utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Objects;

public class StoredVeriBlockBlock {
    public static final int SIZE = 24 + 12 + 32 + 64;
    public static final int CHAIN_WORK_BYTES = 12;

    private final VBlakeHash hash;
    private final VeriBlockBlock block;
    private final BigInteger work;
    private Sha256Hash blockOfProof;

    public VBlakeHash getHash() {
        return hash;
    }

    public VeriBlockBlock getBlock() {
        return block;
    }

    public int getHeight() {
        return block.getHeight();
    }

    public BigInteger getWork() {
        return work;
    }

    public Sha256Hash getBlockOfProof() {
        return blockOfProof;
    }

    public void setBlockOfProof(Sha256Hash blockOfProof) {
        Preconditions.notNull(blockOfProof, "Block of proof cannot be null");
        Preconditions.argument(blockOfProof.length == Sha256Hash.BITCOIN_LENGTH, () -> "Invalid block of proof: " + blockOfProof.toString());
        this.blockOfProof = blockOfProof;
    }

    public StoredVeriBlockBlock(VeriBlockBlock block, BigInteger work) {
        this(block, work, Sha256Hash.ZERO_HASH);
    }

    public StoredVeriBlockBlock(VeriBlockBlock block, BigInteger work, Sha256Hash blockOfProof) {
        Preconditions.notNull(block, "Block cannot be null");
        Preconditions.notNull(work, "Work cannot be null");
        Preconditions.argument(work.compareTo(BigInteger.ZERO) >= 0, "Work must be positive");
        Preconditions.notNull(blockOfProof, "Block of proof cannot be null");

        this.hash = block.getHash();
        this.block = block;
        this.work = work;
        this.blockOfProof = blockOfProof;
    }

    public void serialize(ByteBuffer buffer) {
        buffer.put(hash.getBytes());
        buffer.put(Utils.toBytes(work, CHAIN_WORK_BYTES));
        buffer.put(blockOfProof.getBytes());
        buffer.put(SerializeDeserializeService.serializeHeaders(block));
    }

    public byte[] serialize() {
        ByteBuffer local = ByteBuffer.allocateDirect(SIZE);
        serialize(local);

        local.flip();
        byte[] serialized = new byte[SIZE];
        local.get(serialized);

        return serialized;
    }

    public int getKeystoneIndex() {
        return block.getHeight() / 20 * 20;
    }

    public static StoredVeriBlockBlock deserializeWithoutHash(ByteBuffer buffer) {
        //Skip Hash
        buffer.position(buffer.position() + VBlakeHash.VERIBLOCK_LENGTH);

        return deserialize(buffer);
    }

    public static StoredVeriBlockBlock deserialize(ByteBuffer buffer) {
        byte[] workBytes = new byte[CHAIN_WORK_BYTES];
        buffer.get(workBytes);
        BigInteger work = new BigInteger(1, workBytes);

        byte[] blockOfProofBytes = new byte[Sha256Hash.BITCOIN_LENGTH];
        buffer.get(blockOfProofBytes);
        Sha256Hash blockOfProof = Sha256Hash.wrap(blockOfProofBytes);

        byte[] blockBytes = new byte[Constants.HEADER_SIZE_VeriBlockBlock];
        buffer.get(blockBytes);
        VeriBlockBlock block = SerializeDeserializeService.parseVeriBlockBlock(blockBytes);

        return new StoredVeriBlockBlock(block, work, blockOfProof);
    }

    public static StoredVeriBlockBlock deserialize(byte[] bytes) {
        Preconditions.notNull(bytes, "Raw VeriBlock Block cannot be null");
        Preconditions.argument(bytes.length >= SIZE, () -> "Invalid raw VeriBlock Block: " + Utils.encodeHex(bytes));

        ByteBuffer local = ByteBuffer.allocateDirect(SIZE);
        local.put(bytes, bytes.length - SIZE, SIZE);

        local.flip();
        local.position(VBlakeHash.VERIBLOCK_LENGTH);

        return deserialize(local);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredVeriBlockBlock that = (StoredVeriBlockBlock) o;
        return Objects.equals(hash, that.hash) &&
                Objects.equals(block, that.block) &&
                Objects.equals(work, that.work) &&
                Objects.equals(blockOfProof, that.blockOfProof);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, block, work, blockOfProof);
    }
}
