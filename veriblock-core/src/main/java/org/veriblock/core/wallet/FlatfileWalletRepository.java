// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.core.wallet;

import org.veriblock.core.contracts.FileProvider;
import org.veriblock.core.contracts.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.core.utilities.AddressUtility;

import java.io.*;
import java.util.Scanner;


public class FlatfileWalletRepository implements WalletRepository {
    private static final Logger _logger = LoggerFactory.getLogger(FlatfileWalletRepository.class);
    private static final String DEFAULT_ADDRESS_CONFIG_LINE = "default_address";

    private static final String DEFAULT_WALLET_NAME = "wallet.dat";
    private static final String DEFAULT_WALLET_BACKUP_NAME = "walletbackup.dat";
    private static final String DEFAULT_WALLET_CONFIG_NAME = "walletconfig.dat";

    private final String walletName;
    private final String walletBackupName;
    private final String walletConfigName;
    private final FileProvider fileProvider;


    /**
     * Creates a FlatfileWalletRepository with custom wallet name, wallet backup name, and wallet configuration name
     */
    public FlatfileWalletRepository(String walletName, String walletBackupName, String walletConfigName, FileProvider fileProvider) {
        this.walletName = walletName;
        this.walletBackupName = walletBackupName;
        this.walletConfigName = walletConfigName;
        this.fileProvider = fileProvider;
    }

    /**
     * Creates a FlatfileWalletRepository with the default wallet name, wallet backup name, and wallet configuration name
     */
    public FlatfileWalletRepository(FileProvider fileProvider) {
        this.walletName = DEFAULT_WALLET_NAME;
        this.walletBackupName = DEFAULT_WALLET_BACKUP_NAME;
        this.walletConfigName = DEFAULT_WALLET_CONFIG_NAME;
        this.fileProvider = fileProvider;
    }

    /**
     * Returns the entire byte-encoded wallet
     * @return A byte-encoded wallet
     */
    @Override
    public byte[] getWallet() {
        File walletFile = fileProvider.getDataFile(walletName);
        if (!walletFile.exists()) {
            return null;
        }
        try (FileInputStream walletIn = new FileInputStream(walletFile)) {
            byte[] wallet = new byte[(int)walletFile.length()];
            walletIn.read(wallet);
            return wallet;
        } catch (IOException e) {
            _logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String getWalletConfig() {
        return DEFAULT_ADDRESS_CONFIG_LINE + " " + getDefaultAddress();
    }

    /**
     * Gets the default address specified in the wallet configuration file
     * @return The default address specified by the wallet configuration file
     */
    @Override
    public String getDefaultAddress() {
        File walletConfigFile = fileProvider.getDataFile(walletConfigName);
        if (!walletConfigFile.exists()) {
            return null;
        }

        try (Scanner scan = new Scanner(walletConfigFile)) {
            String line = scan.nextLine();
            while (!line.contains("default_address")) {
                if (scan.hasNextLine()) {
                    line = scan.nextLine();
                } else {
                    _logger.info("Note: there is no default_address configuration option in " + walletConfigFile.getAbsolutePath() + "!");
                    return null;
                }
            }

            // default_address ADDRESS
            String[] parts = line.split(" ");
            if (parts.length < 2) {
                _logger.error("The line \"" + line + "\" is not a valid format for default_address declaration!");
                return null;
            }

            if (AddressUtility.isValidStandardOrMultisigAddress(parts[1])) {
                return parts[1];
            } else {
                _logger.error("The specified default_address " + parts[1] + " is not a valid standard or multisig address!");
                return null;
            }
        } catch (IOException e) {
            _logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Sets the wallet to the provided byte[]
     * @param data The byte[] of the wallet
     * @throws IOException
     */
    @Override
    public void setWallet(byte[] data) throws IOException {
        File walletFile = fileProvider.getDataFile(walletName);

        if (walletFile.exists()) {
            File walletBackupFile = fileProvider.getDataFile(walletBackupName);
            if (walletBackupFile.exists()) {
                walletBackupFile.delete();
            }
            walletFile.renameTo(fileProvider.getDataFile(walletBackupName));
        }

        if (data == null) {
            return;
        }

        try (FileOutputStream walletOut = new FileOutputStream(walletFile)) {
            walletOut.write(data);
        } catch (IOException e) {
            _logger.error("Unable to save the wallet file " + walletFile.getAbsolutePath() + "!", e);
            throw e;
        }
    }

    /**
     * Sets the default address in the wallet config file to the provided address
     * @param address New default address
     * @throws IOException
     */
    @Override
    public void setDefaultAddress(String address) throws IOException {
        if (!AddressUtility.isValidStandardOrMultisigAddress(address) && address != null) {
            _logger.error("setDefaultAddress was called with an invalid address!");
            throw new IllegalArgumentException("setDefaultAddress was called with an invalid address!");
        }

        PrintWriter walletConfigOut = null;
        File walletConfigFile = fileProvider.getDataFile(walletConfigName);
        try {
             walletConfigOut = new PrintWriter(walletConfigFile);

            if (address != null) {
                walletConfigOut.println(DEFAULT_ADDRESS_CONFIG_LINE + " " + address);
            }

            walletConfigOut.close();
        } catch (IOException e) {
            _logger.error("Unable to save the wallet config file " + walletConfigFile.getAbsolutePath() + "!", e);
            throw e;
        } finally {
            if (walletConfigOut != null) {
                walletConfigOut.close();
            }
        }
    }
}
