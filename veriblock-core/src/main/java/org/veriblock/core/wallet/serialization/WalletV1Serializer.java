// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet.serialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.SharedConstants;
import org.veriblock.core.wallet.WalletUnreadableException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class WalletV1Serializer implements WalletSerializer {
    private static final Logger logger = LoggerFactory.getLogger(WalletV1Serializer.class);

    private static final int WALLET_VERSION = 0x01;
    private static final int KEY_TYPE = 0x01;

    @Override
    public StoredWallet read(InputStream inputStream) throws WalletUnreadableException {
        byte[] rawWallet = null;
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            rawWallet = buffer.toByteArray();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return parseRaw(rawWallet);
    }

    @Override
    public void write(StoredWallet wallet, OutputStream outputStream) {

    }

    private StoredWallet parseRaw(byte[] raw) throws WalletUnreadableException {
        if (raw == null) {
            throw new WalletUnreadableException("Could not read bytes from file");
        }

        StoredWallet wallet = new StoredWallet();
        wallet.version = WALLET_VERSION;
        wallet.keyType = KEY_TYPE;

        try (InputStream inputStream = new ByteArrayInputStream(raw)) {
            /* Open the wallet file with byte-level access */


            /* Read the first byte of the wallet file to determine compatibility.
             * Future versions of the VeriBlock codebase will be capable of reading older (legacy) wallets,
             * but will re-write the wallet with the most up-to-date format when saving.
             *
             * As this is the first functional version of the VeriBlock codebase, only one version of wallet
             * is accepted. The version of the wallet does NOT correspond directly to the version of the codebase;
             * any changes to the codebase that don't directly impact the wallet format will not change the latest
             * wallet version number.
             */
            int walletVersion = inputStream.read();

            /*
             * Read the second byte of the wallet file to determine compatibility.
             * Future version of the VeriBlock codebase may offer several different key types (different curves,
             * quantum-computer-resistant, etc.)
             */
            int keyType = inputStream.read();

            if (walletVersion != WALLET_VERSION || keyType != KEY_TYPE) {
                logger.error("Wallet data is in an incorrect format");
                throw new WalletUnreadableException("Wallet file format is incorrect");
            }

            List<StoredAddress> addresses = new ArrayList<>();
            while (inputStream.available() > 0) {
                /* Get the lengths of both the private and public keys */
                int privateKeyLength = inputStream.read();
                int pubKeyLength = inputStream.read();

                /* Create empty byte arrays to store the keys */
                byte[] privateKey = new byte[privateKeyLength];
                byte[] publicKey = new byte[pubKeyLength];

                /* Read in the private and public keys */
                inputStream.read(privateKey);
                inputStream.read(publicKey);

                StoredAddress address = new StoredAddress();
                address.publicKey = publicKey;
                address.cipher = new EncryptedInfo();
                address.cipher.cipherText = privateKey;

                addresses.add(address);
            }

            wallet.addresses = addresses;

        } catch (IOException e) {
            logger.error("An unhandled exception reading the wallet data", e);

            throw new WalletUnreadableException("Could not read from wallet file");
        }

        return wallet;
    }
}
