// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.wallet.serialization.EncryptedInfo;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.concurrent.locks.ReentrantLock;

public class EncryptionManager {
    private static final Logger logger = LoggerFactory.getLogger(EncryptionManager.class);

    private static final int ITERATIONS = 4096;
    private static final int AES_KEY_SIZE = 128;
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int SALT_LEN = 8;

    public static EncryptedInfo encrypt(byte[] data, char[] passphrase) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        EncryptedInfo encryptedInfo = new EncryptedInfo();

        SecureRandom random = SecureRandom.getInstanceStrong();
        encryptedInfo.salt = new byte[SALT_LEN];
        random.nextBytes(encryptedInfo.salt);

        KeySpec keySpec = new PBEKeySpec(passphrase, encryptedInfo.salt, ITERATIONS, AES_KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        SecretKey secretKey = factory.generateSecret(keySpec);

        Cipher cipher = Cipher.getInstance("AES/GCM/PKCS5Padding");
        encryptedInfo.iv = new byte[GCM_NONCE_LENGTH];
        random.nextBytes(encryptedInfo.iv);

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedInfo.iv);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKey.getEncoded(), 0, 16, "AES"), spec);

        encryptedInfo.additionalData = new byte[8];
        random.nextBytes(encryptedInfo.additionalData);
        cipher.updateAAD(encryptedInfo.additionalData);

        encryptedInfo.cipherText = cipher.doFinal(data);

        return encryptedInfo;
    }

    public static byte[] decrypt(EncryptedInfo encrypted, char[] passphrase) {
        if (encrypted.salt == null || encrypted.iv == null || encrypted.additionalData == null) {
            return encrypted.cipherText;
        }

        try {
            KeySpec keySpec = new PBEKeySpec(passphrase, encrypted.salt, ITERATIONS, AES_KEY_SIZE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            SecretKey secretKey = factory.generateSecret(keySpec);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, encrypted.iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKey.getEncoded(), 0, 16, "AES"), spec);
            cipher.updateAAD(encrypted.additionalData);

            return cipher.doFinal(encrypted.cipherText);
        } catch (AEADBadTagException e) {
            throw new WalletLockedException("Invalid password supplied");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }
}
