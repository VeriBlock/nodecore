// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.blockchain.store;

import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.Constants;
import org.veriblock.sdk.models.Sha256Hash;
import org.veriblock.sdk.services.SerializeDeserializeService;
import org.veriblock.sdk.util.Preconditions;
import org.veriblock.sdk.util.Utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Objects;

public class StoredBitcoinBlock {
    public static final int SIZE = 32 + 4 + 12 + 80;
    public static final int CHAIN_WORK_BYTES = 12;

    private final Sha256Hash hash;
    private final BitcoinBlock block;
    private final int height;
    private final BigInteger work;

    public Sha256Hash getHash() {
        return hash;
    }

    public BitcoinBlock getBlock() {
        return block;
    }

    public int getHeight() {
        return height;
    }

    public BigInteger getWork() {
        return work;
    }

    public StoredBitcoinBlock(BitcoinBlock block, BigInteger work, int blockIndex) {
        Preconditions.notNull(block, "Block cannot be null");
        Preconditions.notNull(work, "Work cannot be null");
        Preconditions.argument(work.compareTo(BigInteger.ZERO) >= 0, "Work must be positive");
        Preconditions.argument(blockIndex >= 0, "Block index must be positive");

        this.hash = block.getHash();
        this.block = block;
        this.height = blockIndex;
        this.work = work;
    }

    public void serialize(ByteBuffer buffer) {
        buffer.put(hash.getBytes());
        buffer.putInt(height);
        buffer.put(Utils.toBytes(work, CHAIN_WORK_BYTES));
        buffer.put(SerializeDeserializeService.getHeaderBytesBitcoinBlock(block));
    }

    public byte[] serialize() {
        ByteBuffer local = ByteBuffer.allocateDirect(SIZE);
        serialize(local);

        local.flip();
        byte[] serialized = new byte[SIZE];
        local.get(serialized);

        return serialized;
    }

    public static StoredBitcoinBlock deserializeWithoutHash(ByteBuffer buffer) {
        //Skip Hash
        buffer.position(buffer.position() + Sha256Hash.BITCOIN_LENGTH);

        return deserialize(buffer);
    }

    public static StoredBitcoinBlock deserialize(ByteBuffer buffer) {
        int index = buffer.getInt();

        byte[] workBytes = new byte[CHAIN_WORK_BYTES];
        buffer.get(workBytes);
        BigInteger work = new BigInteger(1, workBytes);

        byte[] blockBytes = new byte[Constants.HEADER_SIZE_BitcoinBlock];
        buffer.get(blockBytes);
        BitcoinBlock block = SerializeDeserializeService.parseBitcoinBlock(blockBytes);

        return new StoredBitcoinBlock(block, work, index);
    }

    public static StoredBitcoinBlock deserialize(byte[] bytes) {
        Preconditions.notNull(bytes, "Raw Bitcoin Block cannot be null");
        Preconditions.argument(bytes.length >= SIZE, () -> "Invalid raw Bitcoin Block: " + Utils.encodeHex(bytes));

        ByteBuffer local = ByteBuffer.allocateDirect(SIZE);
        local.put(bytes, bytes.length - SIZE, SIZE);

        local.flip();
        local.position(Sha256Hash.BITCOIN_LENGTH);

        return deserialize(local);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredBitcoinBlock that = (StoredBitcoinBlock) o;
        return height == that.height &&
                Objects.equals(hash, that.hash) &&
                Objects.equals(block, that.block) &&
                Objects.equals(work, that.work);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, block, height, work);
    }
}
