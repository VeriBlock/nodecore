// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.SharedConstants;
import org.veriblock.core.crypto.Entropy;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class AddressKeyGenerator {
    private static final Logger logger = LoggerFactory.getLogger(AddressKeyGenerator.class);

    private static KeyPairGenerator keyPairGenerator;
    /* KeyFactory can construct public and private keys */
    private static KeyFactory keyFactory;

    static {
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyFactory = KeyFactory.getInstance("EC");

            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

            /* Add another source of entropy (from system event timing) to the SecureRandom PRNG */
            Entropy entropy = new Entropy();
            entropy.entropize();

            /* 'Setting' the seed of a SecureRandom PRNG doesn't discard previous seed information, but
             * simply adds to it. Even *if* the entropy added was low-quality, it won't hurt. */
            random.setSeed(entropy.nextLong());

            /* Initialize the KeyPairGenerator to create 256-bit ECDSA-secp256k1.
             * Bitcoin + co. use ECDSA-secp256k1 (a Koblitz curve). While mathematically secp256r1 is believed to be marginally
             * more secure (~1%), the secp256k1 curve is more 'rigid', and the choosing of apparently 'random' parameters
             * for secp256r1 are suspicious--NIST claims that the 'random' parameters are more efficient, despite
             * evidence that other prime choices are more efficient. */
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256k1"), random);
        } catch (NoSuchAlgorithmException e) {
            /* ECDSA not available */
            logger.error("VERIBLOCK CANNOT FUNCTION WITHOUT ELLIPTICAL CURVE CRYPTOGRAPHY! EXITING.", e);
            System.exit(SharedConstants.Errors.ERROR_NO_ELLIPTICAL_CRYPTOGRAPHY);
        } catch (InvalidAlgorithmParameterException e) {
            /* The secp256k1 curve is not available */
            logger.error("VERIBLOCK CANNOT FUNCTION WITHOUT THE SECP256K1 ELLIPTICAL CURVE! EXITING.", e);
            System.exit(SharedConstants.Errors.ERROR_NO_SECP_256_K_1_ELLIPTICAL_CRYPTOGRAPHY);
        }
    }

    public static KeyPair generate() {
        return keyPairGenerator.generateKeyPair();
    }

    public static PublicKey getPublicKey(byte[] rawPublicKey) throws InvalidKeySpecException {
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(rawPublicKey);
        return keyFactory.generatePublic(publicKeySpec);
    }

    public static PrivateKey getPrivateKey(byte[] rawPrivateKey) throws InvalidKeySpecException {
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(rawPrivateKey);
        return keyFactory.generatePrivate(privateKeySpec);
    }
}