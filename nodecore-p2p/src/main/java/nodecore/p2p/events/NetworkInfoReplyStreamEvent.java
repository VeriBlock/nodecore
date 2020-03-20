// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class NetworkInfoReplyStreamEvent extends StreamEvent<VeriBlockMessages.NetworkInfoReply> {
    private final VeriBlockMessages.NetworkInfoReply networkInfoReply;

    @Override
    public VeriBlockMessages.NetworkInfoReply getContent() {
        return networkInfoReply;
    }

    public NetworkInfoReplyStreamEvent(Peer producer,
                                       String messageId,
                                       boolean acknowledgeRequested,
                                       VeriBlockMessages.NetworkInfoReply networkInfoReply) {
        super(producer, messageId, acknowledgeRequested);
        this.networkInfoReply = networkInfoReply;
    }
}
