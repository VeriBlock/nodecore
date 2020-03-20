// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

// TODO: BloomFilter implementation
package org.veriblock.core.crypto;

import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.function.Function;
import java.util.function.Supplier;

public class BloomFilter {
    private final static double LN2 = Math.log(2.0);
    private final static double LN2_SQUARED = Math.pow(LN2, 2);
    private final static int MAX_SIZE = 36000;
    private final static int MAX_HASHES = 50;
    private final static int SEED = 0xFBA4C795;


    // DO NOT REORDER
    public enum Flags {
        BLOOM_UPDATE_NONE(0),
        BLOOM_UPDATE_ENDORSED_BLOCK(1);

        public final int Value;

        private Flags(int value) {
            Value = value;
        }
    }

    private final BitSet bits;
    private final int hashIterations;
    private final int tweak;

    private boolean empty;

    public int getHashIterations() {
        return hashIterations;
    }
    public int getTweak() {
        return tweak;
    }
    public boolean isEmpty() {
        return empty;
    }
    private void setEmpty(boolean empty) {
        this.empty = empty;
    }

    private BloomFilter(int hashIterations, int tweak, byte[] data) {
        this.bits = BitSet.valueOf(data);
        this.hashIterations = hashIterations;
        this.tweak = tweak;
        setEmpty(false);
    }

    public BloomFilter(int elementCount, double falsePositiveRate, int tweak) {
        this.bits = new BitSet((int)Math.min((-1 / LN2_SQUARED * elementCount * Math.log(falsePositiveRate)), MAX_SIZE * 8) / 8);
        this.hashIterations = Math.min((int)(this.bits.size() * 8 / LN2), MAX_HASHES);
        this.tweak = tweak;
        setEmpty(true);
    }

    public boolean isWithinConstraints() {
        return bits.size() > 0 && bits.size() <= MAX_SIZE &&
                hashIterations > 0 && hashIterations <= MAX_HASHES;
    }

    public void clear() {
        bits.clear();
        setEmpty(true);
    }

    public void insert(byte[] input) {
        insert(seed -> Murmur3.hashBytes(seed, input));
    }

    public void insert(String input) {
        insert(seed -> Murmur3.hashString(seed, input, Charset.forName("UTF-8")));
    }

    public void insert(int input) {
        insert(seed -> Murmur3.hashInt(seed, input));
    }

    public void insert(long input) {
        insert(seed -> Murmur3.hashLong(seed, input));
    }

    private void insert(Function<Integer, Integer> hashingFunction) {
        for (int i = 0; i < getHashIterations(); i++) {
            int hash = hashingFunction.apply(i * SEED + getTweak());

            set(hash);
        }
        setEmpty(false);
    }

    public boolean contains(String input) {
        return contains(seed -> Murmur3.hashString(seed, input, Charset.forName("UTF-8")));
    }

    public boolean contains(byte[] input) {
        return contains(seed -> Murmur3.hashBytes(seed, input));
    }

    public boolean contains(int input) {
        return contains(seed -> Murmur3.hashInt(seed, input));
    }

    public boolean contains(long input) {
        return contains(seed -> Murmur3.hashLong(seed, input));
    }

    private boolean contains(Function<Integer, Integer> hashingFunction) {
        if (isEmpty()) return false;

        for (int i = 0; i < getHashIterations(); i++) {
            int hash = hashingFunction.apply(i * SEED + getTweak());
            if (!get(hash)) {
                return false;
            }
        }

        return true;
    }

    public byte[] getBits() {
        return bits.toByteArray();
    }

    private void set(int hash) {
        bits.set((hash & Integer.MAX_VALUE) % bits.size());
    }

    private boolean get(int hash) {
        return bits.get((hash & Integer.MAX_VALUE) % bits.size());
    }

    public static BloomFilter create(int hashIterations, int tweak, byte[] data) {
        return new BloomFilter(hashIterations, tweak, data);
    }
}
