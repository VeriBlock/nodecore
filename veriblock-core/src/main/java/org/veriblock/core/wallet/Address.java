// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet;

import java.security.PublicKey;

public class Address {
    private final String hash;
    private final PublicKey publicKey;

    public Address(String hash, PublicKey publicKey) {
        this.hash = hash;
        this.publicKey = publicKey;
    }

    public String getHash() {
        return hash;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
