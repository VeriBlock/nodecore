// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import org.veriblock.sdk.models.Coin;

public class OutputFactory {
    public static Output create(String address, long amount) {
        if (amount < 1) throw new IllegalArgumentException("Output amounts must be greater than 0");

        AddressLight addressObj = AddressFactory.create(address);
        Coin amountObj = Coin.valueOf(amount);

        return new Output(addressObj, amountObj);
    }
}
