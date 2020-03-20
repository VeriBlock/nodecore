// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts;

import org.veriblock.core.utilities.SerializerUtility;

import java.io.IOException;
import java.io.OutputStream;

public class TransactionAmount {
    private final Long amount;
    public long value() {
        return amount;
    }

    public TransactionAmount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        this.amount = amount;
    }

    public void serializeToStream(OutputStream stream) throws IOException {
        SerializerUtility.writeVariableLengthValueToStream(stream, amount);
    }

    @Override
    public String toString() {
        return amount.toString();
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof TransactionAmount)) {
            return false;
        }

        return amount.equals(((TransactionAmount)obj).value());
    }

    public TransactionAmount add(TransactionAmount value) {
        return new TransactionAmount(this.amount + value.value());
    }

    public TransactionAmount subtract(TransactionAmount value) {
        return new TransactionAmount(this.amount - value.value());
    }
}
