// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import org.veriblock.core.utilities.AddressUtility;

public class AddressFactory {
    public static AddressLight create(String address) {
        if (address == null) throw new IllegalArgumentException("Address cannot be null");

        if (AddressUtility.isValidStandardAddress(address)) {
            return new StandardAddress(address);
        }
        if (AddressUtility.isValidMultisigAddress(address)) {
            return new MultisigAddress(address);
        }

        throw new IllegalArgumentException("Supplied argument is not a valid address");
    }
}
