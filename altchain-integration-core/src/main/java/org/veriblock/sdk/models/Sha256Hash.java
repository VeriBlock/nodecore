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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Sha256Hash implements Comparable<Sha256Hash> {
    public static final int BITCOIN_LENGTH = 32; // bytes
    public static final int VERIBLOCK_MERKLE_ROOT_LENGTH = 16;
    public static final Sha256Hash ZERO_HASH = wrap(new byte[BITCOIN_LENGTH]);

    public final int length;
    private final byte[] bytes;

    private Sha256Hash(byte[] rawHashBytes, int length) {
        Preconditions.notNull(rawHashBytes, "Sha256 hash cannot be null");
        Preconditions.argument(rawHashBytes.length == length, () -> "Invalid Sha256 hash: " + Utils.encodeHex(rawHashBytes));
        this.length = length;
        this.bytes = rawHashBytes;
    }

    /**
     * Creates a new instance that wraps the given hash value.
     *
     * @param rawHashBytes the raw hash bytes to wrap
     * @return a new instance
     * @throws IllegalArgumentException if the given array length is not exactly 32
     */
    public static Sha256Hash wrap(byte[] rawHashBytes) {
        return new Sha256Hash(rawHashBytes, BITCOIN_LENGTH);
    }

    /**
     * Creates a new instance that wraps the given hash value.
     *
     * @param rawHashBytes the raw hash bytes to wrap
     * @param length the expected length;
     * @return a new instance
     * @throws IllegalArgumentException if the given array length is not exactly length
     */
    public static Sha256Hash wrap(byte[] rawHashBytes, int length) {
        return new Sha256Hash(rawHashBytes, length);
    }

    /**
     * Creates a new instance that wraps the given hash value (represented as a hex string).
     *
     * @param hexString a hash value represented as a hex string
     * @return a new instance
     * @throws IllegalArgumentException if the given string is not a valid
     *         hex string, or if it does not represent exactly 32 bytes
     */
    public static Sha256Hash wrap(String hexString) {
        return wrap(Utils.decodeHex(hexString));
    }

    /**
     * Creates a new instance that wraps the given hash value (represented as a hex string).
     *
     * @param hexString a hash value represented as a hex string
     * @param length the expected length;
     * @return a new instance
     * @throws IllegalArgumentException if the given string is not a valid
     *         hex string, or if it does not represent exactly length
     */
    public static Sha256Hash wrap(String hexString, int length) {
        return wrap(Utils.decodeHex(hexString), length);
    }

    /**
     * Creates a new instance that wraps the given hash value, but with byte order reversed.
     *
     * @param rawHashBytes the raw hash bytes to wrap
     * @return a new instance
     * @throws IllegalArgumentException if the given array length is not exactly 32
     */
    public static Sha256Hash wrapReversed(byte[] rawHashBytes) {
        return wrap(Utils.reverseBytes(rawHashBytes));
    }

    /**
     * Creates a new instance that wraps the given hash value, but with byte order reversed.
     *
     * @param rawHashBytes the raw hash bytes to wrap
     * @param length the length of this hash
     * @return a new instance
     * @throws IllegalArgumentException if the given array length is not exactly 32
     */
    public static Sha256Hash wrapReversed(byte[] rawHashBytes, int length) {
        return wrap(Utils.reverseBytes(rawHashBytes), length);
    }

    /**
     * Creates a new instance that wraps the given hash value, but with byte order reversed.
     *
     * @param hexString a hash value represented as a hex string
     * @return a new instance
     * @throws IllegalArgumentException if the given array length is not exactly 32
     */
    public static Sha256Hash wrapReversed(String hexString) {
        return wrap(Utils.reverseBytes(Utils.decodeHex(hexString)));
    }

    /**
     * Creates a new instance containing the calculated (one-time) hash of the given bytes.
     *
     * @param contents the bytes on which the hash value is calculated
     * @return a new instance containing the calculated (one-time) hash
     */
    public static Sha256Hash of(byte[] contents) {
        return wrap(hash(contents));
    }

    /**
     * Creates a new instance containing the calculated (one-time) hash of the given bytes.
     *
     * @param first the bytes on which the hash value is calculated
     * @param second the bytes on which the hash value is calculated
     * @return a new instance containing the calculated (one-time) hash
     */
    public static Sha256Hash of(byte[] first, byte[] second) {
        return wrap(hash(first, 0, first.length, second, 0, second.length));
    }

    /**
     * Creates a new instance containing the hash of the calculated hash of the given bytes.
     *
     * @param contents the bytes on which the hash value is calculated
     * @return a new instance containing the calculated (two-time) hash
     */
    public static Sha256Hash twiceOf(byte[] contents) {
        return wrap(hashTwice(contents));
    }

    /**
     * Creates a new instance containing the hash of the calculated hash of the given bytes.
     */
    public static Sha256Hash twiceOf(byte[] first, byte[] second) {
        return wrap(hashTwice(first, 0, first.length, second, 0, second.length));
    }

    public static Sha256Hash extract(ByteBuffer buffer, int length, ByteOrder endianness) {
        byte[] dest = new byte[length];
        buffer.get(dest);

        if (endianness == ByteOrder.LITTLE_ENDIAN) {
            return Sha256Hash.wrapReversed(dest, length);
        } else {
            return Sha256Hash.wrap(dest, length);
        }
    }

    public static Sha256Hash extract(ByteBuffer buffer) {
        return extract(buffer, BITCOIN_LENGTH, ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Returns a new SHA-256 MessageDigest instance.
     *
     * This is a convenience method which wraps the checked
     * exception that can never occur with a RuntimeException.
     *
     * @return a new SHA-256 MessageDigest instance
     */
    public static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    /**
     * Calculates the SHA-256 hash of the given bytes.
     *
     * @param input the bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(byte[] input) {
        return hash(input, 0, input.length);
    }

    /**
     * Calculates the SHA-256 hash of the given byte range.
     *
     * @param input the array containing the bytes to hash
     * @param offset the offset within the array of the bytes to hash
     * @param length the number of bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(byte[] input, int offset, int length) {
        MessageDigest digest = newDigest();
        digest.update(input, offset, length);
        return digest.digest();
    }

    /**
     * Calculates the SHA-256 hash of the given byte range.
     *
     * @param first the array containing the bytes to hash
     * @param offset1 the offset within the array of the bytes to hash
     * @param length1 the number of bytes to hash
     * @return the hash (in big-endian order)
     */
    public static byte[] hash(byte[] first, int offset1, int length1, byte[] second, int offset2, int length2) {
        MessageDigest digest = newDigest();
        digest.update(first, offset1, length1);
        digest.update(second, offset2, length2);
        return digest.digest();
    }

    /**
     * Calculates the SHA-256 hash of the given bytes,
     * and then hashes the resulting hash again.
     *
     * @param input the bytes to hash
     * @return the double-hash (in big-endian order)
     */
    public static byte[] hashTwice(byte[] input) {
        return hashTwice(input, 0, input.length);
    }

    /**
     * Calculates the SHA-256 hash of the given byte range,
     * and then hashes the resulting hash again.
     *
     * @param input the array containing the bytes to hash
     * @param offset the offset within the array of the bytes to hash
     * @param length the number of bytes to hash
     * @return the double-hash (in big-endian order)
     */
    public static byte[] hashTwice(byte[] input, int offset, int length) {
        MessageDigest digest = newDigest();
        digest.update(input, offset, length);
        return digest.digest(digest.digest());
    }

    /**
     * Calculates the hash of hash on the given byte ranges. This is equivalent to
     * concatenating the two ranges and then passing the result to {@link #hashTwice(byte[])}.
     */
    public static byte[] hashTwice(byte[] input1, int offset1, int length1,
                                   byte[] input2, int offset2, int length2) {
        MessageDigest digest = newDigest();
        digest.update(input1, offset1, length1);
        digest.update(input2, offset2, length2);
        return digest.digest(digest.digest());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Arrays.equals(bytes, ((Sha256Hash)o).bytes);
    }

    /**
     * Returns the last four bytes of the wrapped hash. This should be unique enough to be a suitable hash code even for
     * blocks, where the goal is to try and get the first bytes to be zeros (i.e. the value as a big integer lower
     * than the target value).
     */
    @Override
    public int hashCode() {
        // Use the last 4 bytes, not the first 4 which are often zeros in Bitcoin.
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

    /**
     * Returns the internal byte array, without defensively copying. Therefore do NOT modify the returned array.
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Returns a reversed copy of the internal byte array.
     */
    public byte[] getReversedBytes() {
        return Utils.reverseBytes(bytes);
    }

    public Sha256Hash getReversed() {
        return new Sha256Hash(Utils.reverseBytes(bytes), bytes.length);
    }

    public Sha256Hash trim(int length) {
        Preconditions.argument(bytes.length >= length, "Invalid trim length");

        if (this.length == length) return this;

        byte[] trimmed = Arrays.copyOfRange(bytes, 0, length);
        return Sha256Hash.wrap(trimmed, length);
    }

    @Override
    public int compareTo(final Sha256Hash other) {
        for (int i = BITCOIN_LENGTH - 1; i >= 0; i--) {
            final int thisByte = this.bytes[i] & 0xff;
            final int otherByte = other.bytes[i] & 0xff;
            if (thisByte > otherByte)
                return 1;
            if (thisByte < otherByte)
                return -1;
        }
        return 0;
    }
}
