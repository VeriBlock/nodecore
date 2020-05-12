// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.util;

import org.veriblock.sdk.transactions.SharedConstants;
import org.veriblock.sdk.transactions.signature.AddressConstants;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;

public class KeyGenerator {
    
    private static final int ADDRESS_DATA_START = AddressConstants.ADDRESS_DATA_START;
    private static final int ADDRESS_DATA_END = AddressConstants.ADDRESS_DATA_END;
    private static final int ADDRESS_CHECKSUM_START = AddressConstants.ADDRESS_CHECKSUM_START;
    private static final int ADDRESS_CHECKSUM_END = AddressConstants.ADDRESS_CHECKSUM_END;
    private static final int ADDRESS_CHECKSUM_LENGTH = ADDRESS_CHECKSUM_END - ADDRESS_CHECKSUM_START;

    private KeyGenerator() { }
    
    public static KeyPair generate() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(1L);
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256k1"), random);
        return keyPairGenerator.generateKeyPair();
    }
    
    public static String addressFromPublicKey(byte[] pubKey) {
        Crypto crypto = new Crypto();

        /* Calculate the SHA-256 of the public key, encode as base-58, take the first 24 characters, prepend a 'V' for VeriBlock */
        String address = SharedConstants.STARTING_CHAR + crypto.SHA256ReturnBase58(pubKey).substring(ADDRESS_DATA_START, ADDRESS_DATA_END);

        /* Append a five-character base-58 checksum */
        address += chopChecksumStandard(crypto.SHA256ReturnBase58(address));

        return address;
    }
    
    private static String chopChecksumStandard(String checksum) {

        if (checksum == null) {
            throw new IllegalArgumentException("getChecksumPortionFromAddress cannot be called with a null checksum!");
        }
        if (checksum.length() < ADDRESS_CHECKSUM_LENGTH) {
            throw new IllegalArgumentException("getChecksumPortionFromAddress cannot be called with an checksum " +
                    "(" + checksum + ") which is not at least " + ADDRESS_CHECKSUM_LENGTH + " characters long!");
        }
        return checksum.substring(0, ADDRESS_CHECKSUM_LENGTH + 1);
    }
}
