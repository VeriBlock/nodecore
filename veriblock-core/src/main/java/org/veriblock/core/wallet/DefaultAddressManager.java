// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.contracts.AddressManager;
import org.veriblock.core.types.Pair;
import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.core.utilities.Utility;
import org.veriblock.core.wallet.serialization.EncryptedInfo;
import org.veriblock.core.wallet.serialization.StoredAddress;
import org.veriblock.core.wallet.serialization.StoredWallet;
import org.veriblock.core.wallet.serialization.WalletSerializer;
import org.veriblock.core.wallet.serialization.WalletV1Serializer;
import org.veriblock.core.wallet.serialization.WalletV2Serializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class designed to handle the generation, storage, retrieval, and use of addresses and their corresponding public/private keys.
 * <p>
 * Effectively, this represents "the wallet" in pure terms.
 */
public class DefaultAddressManager implements AddressManager {
    private static final Logger logger = LoggerFactory.getLogger(DefaultAddressManager.class);

    private static final int WALLET_VERSION = 0x02;
    private static final int KEY_TYPE = 0x01;

    private final ReentrantLock lock = new ReentrantLock(true);
    private boolean loaded = false;

    private final StoredWallet wallet = new StoredWallet();
    private final HashMap<String, Address> addresses = new HashMap<>();
    private final HashMap<String, PrivateKey> privateKeys = new HashMap<>();

    private Address defaultAddress;
    private File walletFile;

    private boolean unlocked = false;
    private char[] key;

    @Override
    public boolean isEncrypted() {
        return wallet.locked;
    }

    @Override
    public boolean isLocked() {
        return isEncrypted() && !unlocked;
    }

    public DefaultAddressManager() {
        wallet.version = WALLET_VERSION;
        wallet.keyType = KEY_TYPE;
        wallet.addresses = new ArrayList<>();
    }

    @Override
    public void load(File walletFile) throws IOException {
        if (walletFile == null) {
            throw new IllegalArgumentException("walletFile cannot be null");
        }
        this.walletFile = walletFile;

        if (this.loaded) {
            reset();
        }

        if (walletFile.exists()) {
            StoredWallet loadedWallet = null;

            try (FileInputStream stream = new FileInputStream(walletFile)) {
                WalletSerializer serializer = new WalletV2Serializer();
                loadedWallet = serializer.read(stream);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }

            if (loadedWallet == null) {
                try (FileInputStream stream = new FileInputStream(walletFile)) {
                    WalletSerializer serializer = new WalletV1Serializer();
                    loadedWallet = serializer.read(stream);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            if (loadedWallet == null) {
                throw new WalletUnreadableException("Unable to parse wallet file");
            }

            loadWallet(loadedWallet);
            if (loadedWallet.version < WALLET_VERSION) {
                save();
            }
        } else {
            Address firstAddress = getNewAddress();
            setDefaultAddress(firstAddress.getHash());
        }

        this.loaded = true;
    }

    /**
     * Set the default (primary) address from this wallet.
     *
     */
    @Override
    public void setDefaultAddress(String address) {
        if (!AddressUtility.isValidStandardAddress(address))
            return;

        Address a = addresses.get(address);
        if (a != null) {
            defaultAddress = a;

            try {
                // Persist selection
                wallet.defaultAddress = address;
                save();
            } catch (Exception e) {
                logger.error("Setting the default address failed!");
            }
        } else {
            throw new IllegalArgumentException("The address (" + address + ") was not found in the loaded addresses");
        }
    }

    /**
     * Get the default (primary) address from this wallet.
     *
     * @return The default (primary) address from this wallet.
     */
    @Override
    public Address getDefaultAddress() {
        if (defaultAddress == null) {
            String address = wallet.defaultAddress;
            if (address == null) {
                return null;
            }

            Address a = addresses.get(address);
            if (a != null) {
                defaultAddress = a;
            }
        }
        return defaultAddress;
    }

    /**
     * Save all of the stored public/private keys to the file specified.
     * This method is available for convenience when building out more complex versions with support for multiple wallets,
     * wallet backup files, etc.
     *
     */
    @Override
    public void save() throws IllegalStateException, IOException {
        if (walletFile == null) throw new IllegalStateException("A wallet file has not been loaded");

        try (FileOutputStream stream = new FileOutputStream(walletFile, false)) {
            lock.lock();

            WalletSerializer serializer = new WalletV2Serializer();
            serializer.write(this.wallet, stream);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Pair<Boolean, String> saveWalletToFile(String filePathToSave) {
        File path = new File(filePathToSave);

        StringBuilder walletDatFileLocation;

        if (path.isDirectory()) {
            walletDatFileLocation = new StringBuilder(path + File.separator + "wallet_backup.dat");
        } else {
            String fileName = path.getName();
            String[] parts = fileName.split("\\.");
            String parent = path.getParent();
            if (parts.length == 1) {
                // No extension
                walletDatFileLocation = new StringBuilder((parent != null ? parent + File.separator : "") + parts[0] + ".dat");
            } else {
                // One (or more) extension(s) provided
                walletDatFileLocation = new StringBuilder((parent != null ? parent + File.separator : "") + File.separator + parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    walletDatFileLocation.append(".").append(parts[i]);
                }
            }
        }

        File walletDatFile = new File(walletDatFileLocation.toString());

        if (walletDatFile.exists()) {
            return new Pair<>(false, "The wallet file " + walletDatFile.getAbsolutePath() + " already exists!");
        }

        try (FileOutputStream stream = new FileOutputStream(walletDatFile, false)) {
            lock.lock();

            WalletSerializer serializer = new WalletV2Serializer();
            serializer.write(this.wallet, stream);

            return new Pair<>(true, "Saved file successfully!");
        } catch (IOException e) {
            String errorContext = "An error occurred while attempting to save the wallet to the file " + walletDatFileLocation + "!";
            logger.error(errorContext, e);

            return new Pair<>(false, errorContext + " exception: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Pair<Boolean, String> importWallet(File toImport) throws WalletUnreadableException {
        if (wallet.locked && !unlocked) {
            throw new WalletLockedException("Wallet must be unlocked before importing another wallet");
        }

        if (!toImport.exists()) {
            // Both relative and absolute failed
            return new Pair<>(false, "Unable to load file from provided path: " + toImport.getAbsolutePath());
        }


        try {
            lock.lock();

            StoredWallet importedWallet = null;

            try (FileInputStream stream = new FileInputStream(toImport)) {
                WalletSerializer serializer = new WalletV2Serializer();
                importedWallet = serializer.read(stream);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }

            if (importedWallet == null) {
                try (FileInputStream stream = new FileInputStream(toImport)) {
                    WalletSerializer serializer = new WalletV1Serializer();
                    importedWallet = serializer.read(stream);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            if (importedWallet == null) {
                throw new WalletUnreadableException("Unable to parse wallet file");
            }

            if (importedWallet.locked) {
                return new Pair<>(false, "Cannot import an encrypted wallet without a passphrase");
            }

            importWallet(importedWallet);
            save();

            return new Pair<>(true, "Keys were read from the wallet successfully!");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return new Pair<>(false, "An exception occurred while reading the file: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Pair<Boolean, String> importEncryptedWallet(File toImport, char[] passphrase) throws WalletUnreadableException {
        if (wallet.locked && !unlocked) {
            throw new WalletLockedException("Wallet must be unlocked before importing another wallet");
        }

        if (!toImport.exists()) {
            // Both relative and absolute failed
            return new Pair<>(false, "Unable to load file from provided path: " + toImport.getAbsolutePath());
        }

        try {
            lock.lock();

            StoredWallet importedWallet = null;

            // No fallback to v1 serializer because encryption was not supported
            try (FileInputStream stream = new FileInputStream(toImport)) {
                WalletSerializer serializer = new WalletV2Serializer();
                importedWallet = serializer.read(stream);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }

            if (importedWallet == null) {
                throw new WalletUnreadableException("Unable to parse wallet file");
            }

            importedWallet = unlockWallet(importedWallet, passphrase);
            importWallet(importedWallet);
            save();

            return new Pair<>(true, "Keys were read from the wallet successfully!");
        } catch (WalletLockedException e) {
            logger.error("Passphrase supplied for wallet to be imported is invalid", e);
            return new Pair<>(false, "Passphrase supplied for wallet to be imported is invalid");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return new Pair<>(false, "An exception occurred while reading the file: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean encryptWallet(char[] passphrase) {
        if (wallet.locked) throw new IllegalStateException("Wallet is already encrypted");
        try {
            lock.lock();

            for (StoredAddress a : wallet.addresses) {
                a.cipher = EncryptionManager.encrypt(a.cipher.cipherText, passphrase);
            }

            wallet.locked = true;
            save();

            lock();

            return true;
        } catch (Exception e) {
            logger.error("Unable to encrypt wallet", e.getMessage());
        } finally {
            lock.unlock();
        }

        return false;
    }

    @Override
    public boolean decryptWallet(char[] passphrase) {
        if (!wallet.locked) return true;

        try {
            lock.lock();

            for (StoredAddress a : wallet.addresses) {
                EncryptedInfo info = new EncryptedInfo();
                info.cipherText = EncryptionManager.decrypt(a.cipher, passphrase);
                a.cipher = info;
            }

            wallet.locked = false;
            key = null;
            unlocked = false;
            privateKeys.clear();

            save();
            return true;
        } catch (Exception e) {
            logger.warn("Unable to decrypt wallet", e);
        } finally {
            lock.unlock();
        }

        return false;
    }

    /**
     * Imports the provided private key into the wallet
     */
    @Override
    public Address importKeyPair(byte[] publicKey, byte[] privateKey) {
        if (wallet.locked && !unlocked) {
            throw new WalletLockedException("Wallet must be unlocked before importing keys");
        }

        try {
            KeyPair keyPair = new KeyPair(
                    AddressKeyGenerator.getPublicKey(publicKey),
                    AddressKeyGenerator.getPrivateKey(privateKey));

            StoredAddress storedAddress = createFromKeyPair(keyPair, key);
            if (storedAddress == null) return null;

            return add(keyPair, storedAddress);
        } catch (InvalidKeySpecException e) {
            logger.error("[CRITICAL] Unable to create a keypair from the provided private key!", e);
        } catch (Exception e) {
            logger.error("[CRITICAL] Unable to read the provided private key!", e);
        }

        return null;
    }

    /**
     * Generates a new public-private ECDSA-secp256k1 keypair, and creates an address corresponding to the public key.
     *
     * @return A pair associating the address along with its public/private keypair.
     */
    @Override
    public Address getNewAddress() throws IOException, WalletLockedException {
        if (wallet.locked && !unlocked) {
            throw new WalletLockedException("Wallet must be unlocked before creating a new address");
        }

        KeyPair pair = AddressKeyGenerator.generate();
        StoredAddress storedAddress = createFromKeyPair(pair, key);
        if (storedAddress == null) return null;

        return add(pair, storedAddress);
    }

    /**
     * Returns, if available, the private key associated with a particular address.
     * As expected, this method only returns the private key for an address that is stored by this DefaultAddressManager,
     * and performs no calculations, only a lookup. It is (or at least should be) impossible to reverse an address
     * into a private key.
     *
     * @param address The address to look up the private key for
     * @return The private key corresponding to the address, or null if the private key of the address is unknown
     */
    @Override
    public PrivateKey getPrivateKeyForAddress(String address) {
        if (wallet.locked && !unlocked) {
            throw new WalletLockedException("Wallet must be unlocked before retrieving private key");
        }

        return getPrivateKey(address, key);
    }

    /**
     * Returns, if available, the public key associated with a particular address.
     * As expected, this method only returns the public key for an address that is stored by this DefaultAddressManager,
     * and performs no calculations, only a lookup. It is (or at least should be) impossible to reverse an address
     * into a public key.
     *
     * @param address The address to look up the public key for
     * @return The public key corresponding to the address, or null if the public key of the address is unknown
     */
    @Override
    public PublicKey getPublicKeyForAddress(String address) {
        Address loadedAddress = addresses.get(address);
        if (loadedAddress == null) return null;

        return loadedAddress.getPublicKey();
    }

    /**
     * Signs the provided message with the private key corresponding to the provided address. If the provided
     * address is now owned by this DefaultAddressManager (and thus there is no access to the private key), or if
     * something goes wrong during the signature initialization or signing process, null will be returned.
     * No exceptions will be thrown.
     *
     * @param message A byte[] of the message to sign
     * @param address The address to sign the message with the corresponding private key of
     * @return A byte[] containing the signature of the corresponding private key to the provided address and the provided message
     */
    @Override
    public byte[] signMessage(byte[] message, String address) {
        PrivateKey privateKey = getPrivateKeyForAddress(address);

        return Utility.signMessageWithPrivateKey(message, privateKey);
    }

    /**
     * Gets the number of addresses currently stored by this AddressManager.
     *
     * @return The number of addresses stored by this AddressManager.
     */
    @Override
    public int getNumAddresses() {
        return addresses.size();
    }

    /**
     * Returns all addresses for which this AddressManager has the public/private keypair loaded.
     *
     * @return All of the addresses, stored as Base58 strings in a List, which exist in this wallet
     */
    @Override
    public List<Address> getAll() {
        ArrayList<Address> result = new ArrayList<>();

        try {
            lock.lock();

            Address defaultAddress = getDefaultAddress();
            for (String key : addresses.keySet()) {
                if (key.equals(defaultAddress.getHash())) {
                    result.add(0, defaultAddress);
                } else {
                    result.add(addresses.get(key));
                }
            }

        } finally {
            lock.unlock();
        }

        return result;
    }

    @Override
    public Address get(String address) {
        return addresses.get(address);
    }

    @Override
    public void lock() {
        if (!wallet.locked) return;

        try {
            lock.lock();

            key = null;
            unlocked = false;
            privateKeys.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean unlock(char[] passphrase) {
        if (!wallet.locked) return true;

        try {
            lock.lock();

            key = passphrase;
            unlocked = true;

            for (StoredAddress a : wallet.addresses) {
                byte[] encoded = EncryptionManager.decrypt(a.cipher, passphrase);
                privateKeys.put(a.address, AddressKeyGenerator.getPrivateKey(encoded));
            }

            // TODO: Implement a timer so that it locks automatically
            return true;
        } catch (InvalidKeySpecException | WalletLockedException e) {
            logger.warn("Unable to unlock wallet", e.getMessage(), e);
            lock();

            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void monitor(Address address) {
        addresses.put(address.getHash(), address);
    }

    private Address add(KeyPair pair, StoredAddress storedAddress) throws IOException {
        try {
            lock.lock();

            if (!addresses.containsKey(storedAddress.address)) {
                wallet.addresses.add(storedAddress);
                save();

                addresses.put(storedAddress.address, new Address(storedAddress.address, pair.getPublic()));
                privateKeys.put(storedAddress.address, pair.getPrivate());
            }

            return addresses.get(storedAddress.address);
        } finally {
            lock.unlock();
        }
    }

    private void loadWallet(StoredWallet loaded) {
        if (loaded != null && loaded.addresses != null) {
            try {
                lock.lock();

                wallet.locked = loaded.locked;
                if (wallet.defaultAddress == null) {
                    wallet.defaultAddress = loaded.defaultAddress;
                }

                for (StoredAddress a : loaded.addresses) {
                    try {
                        PublicKey publicKey = AddressKeyGenerator.getPublicKey(a.publicKey);
                        String address = AddressUtility.addressFromPublicKey(publicKey);
                        a.address = address;

                        if (!addresses.containsKey(address)) {
                            addresses.put(address, new Address(address, publicKey));
                            wallet.addresses.add(a);
                            if (wallet.defaultAddress == null) {
                                wallet.defaultAddress = address;
                            }
                        }
                    } catch (InvalidKeySpecException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void importWallet(StoredWallet imported) throws WalletImportException {
        if (imported != null && imported.addresses != null) {
            try {
                lock.lock();

                for (StoredAddress a : imported.addresses) {
                    try {
                        PublicKey publicKey = AddressKeyGenerator.getPublicKey(a.publicKey);
                        PrivateKey privateKey = AddressKeyGenerator.getPrivateKey(a.cipher.cipherText);

                        a.address = AddressUtility.addressFromPublicKey(publicKey);

                        if (wallet.locked) {
                            if (!unlocked) {
                                throw new WalletLockedException("Wallet must be unlocked before importing another wallet");
                            } else {
                                a.cipher = EncryptionManager.encrypt(a.cipher.cipherText, key);
                            }
                        }

                        Address added = add(new KeyPair(publicKey, privateKey), a);
                        logger.info("Imported address {} from wallet", added.getHash());
                    } catch (InvalidKeySpecException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                throw new WalletImportException("Unable to import wallet", e);
            } finally {
                lock.unlock();
            }
        }
    }

    private void reset() {
        wallet.version = WALLET_VERSION;
        wallet.keyType = KEY_TYPE;
        wallet.defaultAddress = null;
        wallet.addresses.clear();

        addresses.clear();
        defaultAddress = null;
    }

    private StoredAddress createFromKeyPair(KeyPair keyPair, char[] passphrase) {
        StoredAddress address = new StoredAddress();
        address.publicKey = keyPair.getPublic().getEncoded();
        address.address = AddressUtility.addressFromPublicKey(keyPair.getPublic());

        EncryptedInfo encrypted = new EncryptedInfo();
        if (wallet.locked) {
            try {
                encrypted = EncryptionManager.encrypt(keyPair.getPrivate().getEncoded(), passphrase);
            } catch (Exception e) {
                logger.error("Unable to encrypt KeyPair", e);
                return null;
            }
        } else {
            encrypted.cipherText = keyPair.getPrivate().getEncoded();
        }

        address.cipher = encrypted;

        return address;
    }

    private PrivateKey getPrivateKey(String address, char[] passphrase) {
        try {
            lock.lock();

            if (unlocked) {
                return privateKeys.get(address);
            } else {
                for (StoredAddress a : wallet.addresses) {
                    if (a.address.equals(address)) {
                        byte[] encoded = EncryptionManager.decrypt(a.cipher, passphrase);
                        return AddressKeyGenerator.getPrivateKey(encoded);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Unable to get private key for address {}", address, e);
        } finally {
            lock.unlock();
        }

        return null;
    }

    private static StoredWallet unlockWallet(StoredWallet wallet, char[] passphrase) throws WalletLockedException {
        for (StoredAddress a : wallet.addresses) {
            EncryptedInfo unlocked = new EncryptedInfo();
            unlocked.cipherText = EncryptionManager.decrypt(a.cipher, passphrase);

            a.cipher = unlocked;
        }

        wallet.locked = false;
        return wallet;
    }
}
