// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.contracts;

import org.veriblock.core.types.Pair;
import org.veriblock.core.wallet.Address;
import org.veriblock.core.wallet.WalletLockedException;
import org.veriblock.core.wallet.WalletUnreadableException;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

public interface AddressManager {

    boolean isEncrypted();

    boolean isLocked();

    /**
     * Load all of the stored public/private keys to the WalletRepository or create if it does not exist.
     *
     */
    void load(File walletFile) throws IOException;

    /**
     * Save all of the stored public/private keys to the WalletRepository.
     *
     */
    void save() throws IOException;

    /**
     * Save all of the stored public/private keys to the specified file
     */
    Pair<Boolean, String> saveWalletToFile(String filePathToSave);

    /**
     * Save all of the stored public/private keys to the specified file
     */
    Pair<Boolean, String> importWallet(File toImport) throws WalletUnreadableException;

    /**
     * Set the default (primary) address from this wallet.
     *
     */
    void setDefaultAddress(String address);

    /**
     * Get the default (primary) address from this wallet.
     *
     * @return The default (primary) address from this wallet.
     */
    Address getDefaultAddress();

    /**
     * Add address for monitoring.
     */
    void monitor(Address address);

    Pair<Boolean, String> importEncryptedWallet(File toImport, char[] passphrase) throws WalletUnreadableException;

    boolean encryptWallet(char[] passphrase);

    boolean decryptWallet(char[] passphrase);

    Address importKeyPair(byte[] privateKey, byte[] publicKey);

    /**
     * Generates a new public-private ECDSA-secp256k1 keypair, and creates an address corresponding to the public key.
     *
     * @return A pair associating the address along with its public/private keypair.
     */
    Address getNewAddress() throws IOException, WalletLockedException;

    /**
     * Returns, if available, the private key associated with a particular address.
     * As expected, this method only returns the private key for an address that is stored by this DefaultAddressManager,
     * and performs no calculations, only a lookup. It is (or at least should be) impossible to reverse an address
     * into a private key.
     *
     * @param address The address to look up the private key for
     * @return The private key corresponding to the address, or null if the private key of the address is unknown
     */
    PrivateKey getPrivateKeyForAddress(String address);

    /**
     * Returns, if available, the public key associated with a particular address.
     * As expected, this method only returns the public key for an address that is stored by this DefaultAddressManager,
     * and performs no calculations, only a lookup. It is (or at least should be) impossible to reverse an address
     * into a public key.
     *
     * @param address The address to look up the public key for
     * @return The public key corresponding to the address, or null if the public key of the address is unknown
     */
    PublicKey getPublicKeyForAddress(String address);

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
    byte[] signMessage(byte[] message, String address);

    /**
     * Gets the number of addresses currently stored by this DefaultAddressManager.
     *
     * @return The number of addresses stored by this DefaultAddressManager.
     */
    int getNumAddresses();

    /**
     * Returns all addresses for which this DefaultAddressManager has the public/private keypair loaded.
     *
     * @return All of the addresses, stored as Base58 strings in a List, which exist in this wallet
     */
    List<Address> getAll();

    /**
     * Returns the address for the specified address hash
     *
     * @return The Address instance if found; otherwise, null
     */
    Address get(String hash);

    void lock();

    boolean unlock(char[] passphrase);
}
