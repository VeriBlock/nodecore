// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p;

public class NodeMetadata {
    private String address;
    public String getAddress() {
        return address;
    }
    void setAddress(String value) {
        address = value;
    }

    private int port;
    public int getPort() {
        return port;
    }
    void setPort(int value) {
        port = value;
    }

    String getAddressKey() {
        return address + ":" + port;
    }

    private String application;
    public String getApplication() { return application; }

    private int protocolVersion;
    public int getProtocolVersion() { return protocolVersion; }

    private String platform;
    public String getPlatform() { return platform; }

    private int startTimestamp;
    public int getStartTimestamp() { return startTimestamp; }

    private boolean canShareAddress;
    public boolean shareAddress() { return canShareAddress; }

    private long capabilities;
    public long getCapabilities() {
        return capabilities;
    }

    private String id;
    public String getId() { return id; }

    private NodeMetadata(String peerAddress, int peerPort) {
        address = peerAddress;
        port = peerPort;
    }

    public static Builder newBuilder() { return new Builder(); }

    public static final class Builder {
        private Builder() {

        }

        private String address;
        public String getAddress() {
            return address;
        }
        public Builder setAddress(String value) {
            address = value;
            return this;
        }

        private int port;
        public int getPort() {
            return port;
        }
        public Builder setPort(int value) {
            port = value;
            return this;
        }

        private String application;
        public String getApplication() { return application != null ? application : "Unknown"; }
        public Builder setApplication(String value) {
            application = value;
            return this;
        }

        private int protocolVersion;
        public int getProtocolVersion() { return protocolVersion; }
        public Builder setProtocolVersion(int value) {
            protocolVersion = value;
            return this;
        }

        private String platform;
        public String getPlatform() { return platform != null ? platform : "Unknown"; }
        public Builder setPlatform(String value) {
            platform = value;
            return this;
        }

        private int startTimestamp;
        public int getStartTimestamp() { return startTimestamp; }
        public Builder setStartTimestamp(int value) {
            startTimestamp = value;
            return this;
        }

        private boolean canShareAddress;
        public boolean shareAddress() {
            return canShareAddress;
        }
        public Builder setShareAddress(boolean value) {
            canShareAddress = value;
            return this;
        }

        private long capabilities;
        public long getCapabilities() {
            return capabilities;
        }
        public Builder setCapabilities(long value) {
            capabilities = value;
            return this;
        }

        private String id;
        public String getId() { return id != null ? id : ""; }
        public Builder setId(String value) {
            id = value;
            return this;
        }

        public NodeMetadata build() {
            NodeMetadata node = new NodeMetadata(getAddress(), getPort());
            node.application = getApplication();
            node.protocolVersion = getProtocolVersion();
            node.platform = getPlatform();
            node.startTimestamp = getStartTimestamp();
            node.canShareAddress = shareAddress();
            node.capabilities = getCapabilities();
            node.id = getId();

            return node;
        }
    }
}
