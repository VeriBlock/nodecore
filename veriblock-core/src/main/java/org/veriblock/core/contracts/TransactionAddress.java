// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts;

import org.veriblock.core.SharedConstants;
import org.veriblock.core.bitcoinj.Base58;
import org.veriblock.core.bitcoinj.Base59;
import org.veriblock.core.utilities.AddressUtility;

import java.io.IOException;
import java.io.OutputStream;

public class TransactionAddress {
    private final String address;
    public String value() {
        return address;
    }
    private final boolean multisig;
    public boolean isMultisig() {
        return multisig;
    }

    public TransactionAddress(String address) {
        if (address == null) throw new IllegalArgumentException("address cannot be null");

        if (AddressUtility.isValidStandardAddress(address)) {
            this.address = address;
            this.multisig = false;
        } else if (AddressUtility.isValidMultisigAddress(address)) {
            this.address = address;
            this.multisig = true;
        } else {
            throw new IllegalArgumentException("address is not a valid address");
        }
    }

    public byte[] toByteArray() {
        return isMultisig() ? Base59.decode(address) : Base58.decode(address);
    }

    public void serializeToStream(OutputStream stream) throws IOException {
        byte[] bytes = toByteArray();

        if (multisig) {
            stream.write(SharedConstants.MULTISIG_ADDRESS_ID);
        } else {
            stream.write(SharedConstants.STANDARD_ADDRESS_ID);
        }

        stream.write((byte) bytes.length);
        stream.write(bytes);
    }

    @Override
    public String toString() {
        return address;
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof TransactionAddress)) {
            return false;
        }

        return address.equals(((TransactionAddress) obj).value());
    }
}
