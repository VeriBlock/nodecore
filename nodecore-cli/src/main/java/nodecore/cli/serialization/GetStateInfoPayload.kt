// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.GetStateInfoReply

class GetStateInfoPayload(
    reply: GetStateInfoReply
) {
    @SerializedName("blockchain_state")
    val blockchainState = reply.blockchainState.state.name

    @SerializedName("operating_state")
    val operatingState = reply.operatingState.state.name

    @SerializedName("network_state")
    val networkState = reply.networkState.state.name

    @SerializedName("connected_peer_count")
    val connectedPeerCount = reply.connectedPeerCount

    @SerializedName("current_sync_peer")
    val currentSyncPeer = reply.currentSyncPeer

    @SerializedName("network_height")
    val networkHeight = reply.networkHeight

    @SerializedName("local_blockchain_height")
    val localBlockchainHeight = reply.localBlockchainHeight

    @SerializedName("network_version")
    val networkVersion = reply.networkVersion

    @SerializedName("data_directory")
    val dataDirectory = reply.dataDirectory

    @SerializedName("program_version")
    val programVersion = reply.programVersion

    @SerializedName("nodecore_starttime")
    val nodecoreStarttime = reply.nodecoreStarttime

    @SerializedName("wallet_cache_sync_height")
    val walletCacheSyncHeight = reply.walletCacheSyncHeight

    @SerializedName("wallet_state")
    val walletState = reply.walletState.name
}
