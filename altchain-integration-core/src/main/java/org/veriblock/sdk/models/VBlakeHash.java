// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

import org.veriblock.sdk.util.Preconditions;
import org.veriblock.sdk.util.Utils;
import org.veriblock.sdk.util.VBlake;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class VBlakeHash {
    public static final int VERIBLOCK_LENGTH = 24;
    public static final int PREVIOUS_BLOCK_LENGTH = 12;
    public static final int PREVIOUS_KEYSTONE_LENGTH = 9;

    public static final VBlakeHash EMPTY_HASH = new VBlakeHash(new byte[VERIBLOCK_LENGTH], VERIBLOCK_LENGTH);

    public final int length;
    private final byte[] bytes;

    private VBlakeHash(byte[] bytes, int length) {
        Preconditions.notNull(bytes, "VBlake hash cannot be null");
        Preconditions.argument(bytes.length == length, () -> "Invalid VBlake hash: " + Utils.encodeHex(bytes));

        this.length = length;
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public VBlakeHash trimToPreviousBlockSize() {
        Preconditions.state(length >= PREVIOUS_BLOCK_LENGTH,
                "Length should be higher than previous block length");

        return VBlakeHash.trim(this, PREVIOUS_BLOCK_LENGTH);
    }

    public VBlakeHash trimToPreviousKeystoneSize() {
        Preconditions.state(length >= PREVIOUS_KEYSTONE_LENGTH,
                "Length should be higher than previous keystone length");

        return VBlakeHash.trim(this, PREVIOUS_KEYSTONE_LENGTH);
    }

    public static VBlakeHash wrap(byte[] bytes) {
        return new VBlakeHash(bytes, VERIBLOCK_LENGTH);
    }

    public static VBlakeHash wrap(String hexHash) {
        return new VBlakeHash(Utils.decodeHex(hexHash), VERIBLOCK_LENGTH);
    }

    public static VBlakeHash wrap(byte[] bytes, int length) {
        return new VBlakeHash(bytes, length);
    }

    public static VBlakeHash wrap(String hexHash, int length) {
        return new VBlakeHash(Utils.decodeHex(hexHash), length);
    }

    public static VBlakeHash extract(ByteBuffer buffer, int length) {
        byte[] dest = new byte[length];
        buffer.get(dest);

        return VBlakeHash.wrap(dest, length);
    }

    public static VBlakeHash hash(byte[] input) {
        return wrap(VBlake.hash(input));
    }

    public static VBlakeHash trim(VBlakeHash input, int length) {
        Preconditions.argument(length <= input.length, "Invalid trim length");

        byte[] output = new byte[length];
        System.arraycopy(input.bytes, input.bytes.length - length, output, 0, length);
        return VBlakeHash.wrap(output, length);
    }

    public boolean probablyEquals(VBlakeHash other) {
        if (other == null) return false;

        return this.hashCode() == other.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof VBlakeHash)) return false;

        return Arrays.equals(bytes, ((VBlakeHash)o).bytes);
    }

    @Override
    public int hashCode() {
        int length = bytes.length;
        return Utils.fromBytes(bytes[length - 4], bytes[length - 3], bytes[length - 2], bytes[length - 1]);
    }

    @Override
    public String toString() {
        return Utils.encodeHex(bytes);
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     */
    public BigInteger toBigInteger() {
        return new BigInteger(1, bytes);
    }
}
