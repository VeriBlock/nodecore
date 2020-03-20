// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;

public class NodeInfo {

    public String address;
    public int port;
    public String application;

    @SerializedName("protocol_version")
    public int protocolVersion;
    public String platform;

    @SerializedName("start_timestamp")
    public int startTimestamp;

    public NodeInfo(VeriBlockMessages.NodeInfo info) {
        address = info.getAddress();
        port = info.getPort();
        application = info.getApplication();
        protocolVersion = info.getProtocolVersion();
        platform = info.getPlatform();
        startTimestamp = info.getStartTimestamp();
    }
}
