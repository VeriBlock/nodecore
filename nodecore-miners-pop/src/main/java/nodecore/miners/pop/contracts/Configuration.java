// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.contracts;

import nodecore.miners.pop.common.BitcoinNetwork;

import java.io.InputStream;
import java.util.List;

public interface Configuration {
    void load();

    void load(InputStream inputStream);

    BitcoinNetwork getBitcoinNetwork();

    long getMaxTransactionFee();

    ConfigurationResult setMaxTransactionFee(String value);

    long getTransactionFeePerKB();

    ConfigurationResult setTransactionFeePerKB(String value);

    boolean isMinimumRelayFeeEnforced();

    ConfigurationResult setMinimumRelayFeeEnforced(String value);

    String getNodeCoreHost();

    ConfigurationResult setNodeCoreHost(String value);

    int getNodeCorePort();

    ConfigurationResult setNodeCorePort(String value);

    String getCronSchedule();

    int getActionTimeout();

    ConfigurationResult setActionTimeout(String value);

    int getHttpApiPort();

    String getHttpApiAddress();

    List<String> list();

    ConfigurationResult setProperty(String key, String value);

    boolean getBoolean(String key);

    ConfigurationResult setBoolean(String key, String value);

    void save();

    boolean isValid();

    boolean getNodeCoreUseSSL();

    ConfigurationResult setNodeCoreUseSSL(String value);

    String getNodeCorePassword();

    ConfigurationResult setNodeCorePassword(String value);

    String getCertificateChainPath();

    ConfigurationResult setCertificateChainPath(String value);

    String getDataDirectory();

    String getDatabasePath();
}
