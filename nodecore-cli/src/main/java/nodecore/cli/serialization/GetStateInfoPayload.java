// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;

public class GetStateInfoPayload {
    public GetStateInfoPayload(final VeriBlockMessages.GetStateInfoReply reply) {
        blockchainState = reply.getBlockchainState().getState().name();
        operatingState = reply.getOperatingState().getState().name();
        networkState = reply.getNetworkState().getState().name();
        connectedPeerCount = reply.getConnectedPeerCount();
        networkHeight = reply.getNetworkHeight();
        currentSyncPeer = reply.getCurrentSyncPeer();
        localBlockchainHeight = reply.getLocalBlockchainHeight();
        networkVersion = reply.getNetworkVersion();
        dataDirectory = reply.getDataDirectory();
        programVersion = reply.getProgramVersion();
        nodecoreStarttime = reply.getNodecoreStarttime();
        walletCacheSyncHeight = reply.getWalletCacheSyncHeight();
        walletState = reply.getWalletState().name();
    }

    @SerializedName("blockchain_state")
    public String blockchainState;

    @SerializedName("operating_state")
    public String operatingState;

    @SerializedName("network_state")
    public String networkState;

    @SerializedName("connected_peer_count")
    public int connectedPeerCount;

    @SerializedName("current_sync_peer")
    public String currentSyncPeer;

    @SerializedName("network_height")
    public int networkHeight;

    @SerializedName("local_blockchain_height")
    public int localBlockchainHeight;

    @SerializedName("network_version")
    public String networkVersion;

    @SerializedName("data_directory")
    public String dataDirectory;

    @SerializedName("program_version")
    public String programVersion;

    @SerializedName("nodecore_starttime")
    public long nodecoreStarttime;

    @SerializedName("wallet_cache_sync_height")
    public int walletCacheSyncHeight;

    @SerializedName("wallet_state")
    public String walletState;
}
