// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.wallet;

import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.core.wallet.AddressKeyGenerator;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

public class Key {

    private final String address;
    private final KeyPair keyPair;

    public String getAddress() {
        return address;
    }

    public byte[] getPublicKey() {
        return keyPair.getPublic().getEncoded();
    }

    public byte[] getPrivateKey() {
        return keyPair.getPrivate().getEncoded();
    }

    public Key(KeyPair keyPair) {
        this.keyPair = keyPair;
        this.address = AddressUtility.addressFromPublicKey(keyPair.getPublic());
    }

    public static Key parse(byte[] publicKeyBytes, byte[] privateKeyBytes) throws InvalidKeySpecException {
        PublicKey publicKey = AddressKeyGenerator.getPublicKey(publicKeyBytes);
        PrivateKey privateKey = AddressKeyGenerator.getPrivateKey(privateKeyBytes);

        return new Key(new KeyPair(publicKey, privateKey));
    }
}
