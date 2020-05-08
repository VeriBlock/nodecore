// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package veriblock.conf;

import org.veriblock.sdk.models.BitcoinBlock;
import org.veriblock.sdk.models.VeriBlockBlock;

/**
 * Configure parameters that depends on a network. (test | main | alfa)
 */
public abstract class NetworkParameters {
    public static final String LOCALHOST = "127.0.0.1";

    protected String adminHost;
    protected Integer adminPort;
    protected Integer p2pPort;
    protected String certificateChainPath;
    protected boolean ssl = false;
    protected String adminPassword;
    protected String bootstrapDns;
    protected String databaseName;

    /**
     * Network name.
     */
    public abstract String getNetworkName();
    /**
     * Genesis block for specific network.
     */
    public abstract VeriBlockBlock getGenesisBlock();
    /**
     * Bitcoin origin block for specific network.
     */
    public abstract BitcoinBlock getBitcoinOriginBlock();
    /**
     * Protocol version.
     */
    public abstract Integer getProtocolVersion();

    public String getAdminHost() {
        return adminHost;
    }
    public void setAdminHost(String adminHost) {
        this.adminHost = adminHost;
    }

    public int getAdminPort() {
        return adminPort;
    }
    public void setAdminPort(int adminPort) {
        this.adminPort = adminPort;
    }

    public void setP2pPort(int p2pPort) {
        this.p2pPort = p2pPort;
    }

    public String getBootstrapDns() {
        return bootstrapDns;
    }

    public Integer getP2pPort() {
        return p2pPort;
    }

    public String getCertificateChainPath() {
        return certificateChainPath;
    }
    public void setCertificateChainPath(String certificateChainPath) {
        this.certificateChainPath = certificateChainPath;
    }

    public boolean isSsl() {
        return ssl;
    }
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getAdminPassword() {
        return adminPassword;
    }
    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getDatabaseName() {
        return databaseName;
    }
}
