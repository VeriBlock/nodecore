// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.auditor;

import org.veriblock.sdk.blockchain.store.StoredVeriBlockBlock;
import org.veriblock.sdk.util.Preconditions;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public abstract class Change {
    //StoredVeriBlockBlock.SIZE = 132
    //StoredBitcoinBlock.SIZE = 128
    public static final int MAX_SIZE = 4 + 2 + 2 + 132 + 132;
    public static final int MAX_HASH_SIZE = StoredVeriBlockBlock.SIZE;

    private byte[] oldValue;
    private byte[] newValue;

    public abstract String getChainIdentifier();

    public abstract Operation getOperation();

    public byte[] getOldValue() {
        return oldValue;
    }

    public byte[] getNewValue() {
        return newValue;
    }

    protected Change(byte[] oldValue, byte[] newValue) {
        Preconditions.argument(oldValue != null && oldValue.length <= MAX_HASH_SIZE, "New value must have a length of at least " + MAX_HASH_SIZE + " bytes");
        Preconditions.argument(newValue != null && newValue.length <= MAX_HASH_SIZE, "Old value must have a length of at least " + MAX_HASH_SIZE + " bytes");

        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(MAX_SIZE);
        buffer.put(getChainIdentifier().getBytes(StandardCharsets.US_ASCII));
        buffer.putShort(getOperation().getValue());
        buffer.putShort((short) oldValue.length);
        buffer.put(oldValue);
        buffer.put(newValue);

        int size = buffer.position();
        buffer.flip();

        byte[] serialized = new byte[size];
        buffer.get(serialized);

        return serialized;
    }

    public static Change deserialize(ByteBuffer buffer) {
        byte[] chainIdentifierBytes = new byte[4];
        buffer.get(chainIdentifierBytes);
        String chainIdentifier = new String(chainIdentifierBytes, StandardCharsets.US_ASCII);

        Optional<Operation> operation = Operation.valueOf(buffer.getShort());
        if (!operation.isPresent()) return null;

        short valueSize = buffer.getShort();
        byte[] oldValue = new byte[valueSize];
        byte[] newValue = new byte[valueSize];

        buffer.get(oldValue);
        buffer.get(newValue);

        return new ReadOnlyChange(chainIdentifier, operation.get(), oldValue, newValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Change change = (Change) o;
        return Arrays.equals(oldValue, change.oldValue) &&
                Arrays.equals(newValue, change.newValue);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(oldValue);
        result = 31 * result + Arrays.hashCode(newValue);
        return result;
    }
}
