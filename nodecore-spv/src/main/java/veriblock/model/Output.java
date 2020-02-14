// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import org.veriblock.sdk.models.Coin;
import org.veriblock.sdk.services.SerializeDeserializeService;

import java.io.IOException;
import java.io.OutputStream;

public class Output {
    private final AddressLight address;
    private final Coin amount;

    public AddressLight getAddress() {
        return address;
    }

    public Coin getAmount() {
        return amount;
    }

    public Output(AddressLight address, Coin amount) {
        this.address = address;
        this.amount = amount;
    }

    public void serializeToStream(OutputStream stream) throws IOException {
        address.serializeToStream(stream);
        SerializeDeserializeService.serialize(amount, stream);
    }
}
