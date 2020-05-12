// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

public class Output {
    private final Address address;
    private final Coin amount;

    public Address getAddress() {
        return address;
    }

    public Coin getAmount() {
        return amount;
    }

    public Output(Address address, Coin amount) {
        this.address = address;
        this.amount = amount;
    }

    public static Output of(String address, long amount) {
        return new Output(new Address(address), Coin.valueOf(amount));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Output obj = (Output)o;

        return address.equals(obj.address) && amount.equals(obj.amount);
    }
}
