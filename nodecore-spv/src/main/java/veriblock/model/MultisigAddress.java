// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.model;

import org.veriblock.core.bitcoinj.Base59;

public class MultisigAddress extends AddressLight {
    @Override
    public byte getType() {
        return (byte)0x03;
    }

    public MultisigAddress(String address) {
        super(address);
    }

    @Override
    public byte[] toByteArray() {
        return Base59.decode(get());
    }
}
