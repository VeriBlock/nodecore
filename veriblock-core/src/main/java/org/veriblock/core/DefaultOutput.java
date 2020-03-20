// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core;

import org.veriblock.core.contracts.Output;
import org.veriblock.core.contracts.TransactionAddress;
import org.veriblock.core.contracts.TransactionAmount;
import org.veriblock.core.utilities.AddressUtility;

import java.io.IOException;
import java.io.OutputStream;

public class DefaultOutput implements Output {
    private final TransactionAmount _amount;
    private final TransactionAddress _address;

    public DefaultOutput(String address, long amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("DefaultOutput constructor cannot be called with a negative or zero output amount (" + amount + ")!");
        }

        if (address == null) {
            throw new IllegalArgumentException("DefaultOutput constructor cannot be called with a null address!");
        }

        if (!AddressUtility.isValidStandardOrMultisigAddress(address)) {
            throw new IllegalArgumentException("DefaultOutput constructor cannot be called with an invalid standard/multisig address (" + address + ")!");
        }

        _address = new TransactionAddress(address);
        _amount = new TransactionAmount(amount);
    }

    public String toString() {
        return "Output[address=" + _address + ", amount=" + _amount + "]";
    }

    @Override
    public TransactionAmount getAmount() {
        return _amount;
    }

    @Override
    public TransactionAddress getAddress() {
        return _address;
    }

    @Override
    public String getAddressString() {
        return _address.value();
    }

    @Override
    public void serializeToStream(OutputStream stream) throws IOException {
        _address.serializeToStream(stream);
        _amount.serializeToStream(stream);
    }
}
