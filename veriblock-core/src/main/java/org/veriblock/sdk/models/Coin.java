// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

import org.veriblock.sdk.util.StreamUtils;
import org.veriblock.sdk.util.Utils;

import java.nio.ByteBuffer;

public final strictfp class Coin implements Comparable<Coin> {
    public static final long COIN_VALUE = 100000000;

    public static final Coin ZERO = Coin.valueOf(0);
    public static final Coin ONE = Coin.valueOf(COIN_VALUE);

    private final long atomicUnits;
    public long getAtomicUnits() {
        return atomicUnits;
    }

    private Coin(long atomicUnits) {
        this.atomicUnits = atomicUnits;
    }

    public Coin add(Coin addend) {
        return new Coin(atomicUnits + addend.atomicUnits);
    }

    public Coin subtract(Coin subtrahend) {
        return new Coin(atomicUnits - subtrahend.atomicUnits);
    }

    public Coin negate() {
        return Coin.ZERO.subtract(this);
    }

    public double toDecimal() {
        return (double) atomicUnits / (double) COIN_VALUE;
    }

    public static Coin valueOf(long atomicUnits) {
        return new Coin(atomicUnits);
    }

    @Override
    public int hashCode() {
        return (int) this.atomicUnits;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || !(obj instanceof Coin)) return false;

        return this.atomicUnits == ((Coin) obj).atomicUnits;
    }

    @Override
    public String toString() {
        return Long.toString(this.atomicUnits);
    }

    @Override
    public int compareTo(Coin o) {
        return Long.compare(atomicUnits, o.atomicUnits);
    }

    public static Coin parse(ByteBuffer txBuffer) {
        long atomicUnits = Utils.toLong(StreamUtils.getSingleByteLengthValue(txBuffer, 0, 8));
        return Coin.valueOf(atomicUnits);
    }
}
