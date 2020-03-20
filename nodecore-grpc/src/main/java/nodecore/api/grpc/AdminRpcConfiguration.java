// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.grpc;

public class AdminRpcConfiguration {
    private boolean ssl;
    public boolean isSsl() {
        return ssl;
    }
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    private String nodeCorePassword;
    public String getNodeCorePassword() {
        return nodeCorePassword;
    }
    public void setNodeCorePassword(String nodeCorePassword) {
        this.nodeCorePassword = nodeCorePassword;
    }

    private String certificateChainPath;
    public String getCertificateChainPath() {
        return certificateChainPath;
    }
    public void setCertificateChainPath(String certificateChainPath) {
        this.certificateChainPath = certificateChainPath;
    }

    private String nodeCoreHost;
    public String getNodeCoreHost() {
        return nodeCoreHost;
    }
    public void setNodeCoreHost(String nodeCoreHost) {
        this.nodeCoreHost = nodeCoreHost;
    }

    private int nodeCorePort;
    public int getNodeCorePort() {
        return nodeCorePort;
    }
    public void setNodeCorePort(int nodeCorePort) {
        this.nodeCorePort = nodeCorePort;
    }
}
