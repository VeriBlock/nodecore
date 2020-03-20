// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.serialization;

import com.google.gson.annotations.SerializedName;
import nodecore.api.grpc.VeriBlockMessages;
import nodecore.cli.contracts.PeerEndpoint;

import java.util.List;
import java.util.stream.Collectors;

public class PeerInfoPayload {

    @SerializedName("configured_endpoints")
    public List<PeerEndpoint> peerEndpoints;
    @SerializedName("connected")
    public List<NodeInfo> connectedNodes;
    @SerializedName("disconnected")
    public List<NodeInfo> disconnectedNodes;
    @SerializedName("candidates")
    public List<NodeInfo> candidateNodes;

    public PeerInfoPayload(final VeriBlockMessages.GetPeerInfoReply reply) {
        peerEndpoints = reply.getEndpointsList()
                .stream()
                .map(x -> new PeerEndpoint(x.getAddress(), (short) x.getPort(), null))
                .collect(Collectors.toList());

        connectedNodes = reply.getConnectedNodesList()
                .stream()
                .map(NodeInfo::new)
                .collect(Collectors.toList());

        disconnectedNodes = reply.getDisconnectedNodesList()
                .stream()
                .map(NodeInfo::new)
                .collect(Collectors.toList());

        candidateNodes = reply.getCandidateNodesList()
                .stream()
                .map(NodeInfo::new)
                .collect(Collectors.toList());
    }
}
