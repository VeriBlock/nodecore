// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.GetPeerInfoReply
import nodecore.cli.contracts.PeerEndpoint

class PeerInfoPayload(
    reply: GetPeerInfoReply
) {
    @SerializedName("configured_endpoints")
    val peerEndpoints = reply.endpointsList.map { endpoint ->
        PeerEndpoint(
            address = endpoint.address,
            port = endpoint.port.toShort()
        )
    }

    @SerializedName("connected")
    val connectedNodes = reply.connectedNodesList.map { nodeInfo ->
        NodeInfo(nodeInfo)
    }

    @SerializedName("disconnected")
    val disconnectedNodes = reply.disconnectedNodesList.map { nodeInfo ->
        NodeInfo(nodeInfo)
    }

    @SerializedName("candidates")
    val candidateNodes = reply.candidateNodesList.map { nodeInfo ->
        NodeInfo(nodeInfo)
    }
}
