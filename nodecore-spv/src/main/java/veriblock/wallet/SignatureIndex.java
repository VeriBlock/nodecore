// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.wallet;

public class SignatureIndex {
    private long value;
    public long get() {
        return value;
    }
    public void set(long value) {
        this.value = value;
    }

    private Long pendingValue;
    public long getPending() {
        if (pendingValue != null && pendingValue > value) {
            return pendingValue;
        }

        return value;
    }
    public void setPending(long value) {
        this.pendingValue = value;
    }

    public SignatureIndex(long startingValue) {
        this.value = startingValue;
        this.pendingValue = startingValue;
    }
}
