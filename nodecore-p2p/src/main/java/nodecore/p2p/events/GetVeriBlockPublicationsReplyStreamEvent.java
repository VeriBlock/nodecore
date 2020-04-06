// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class GetVeriBlockPublicationsReplyStreamEvent extends StreamEvent<VeriBlockMessages.GetVeriBlockPublicationsReply> {
    private final VeriBlockMessages.GetVeriBlockPublicationsReply veriBlockPublicationsReply;

    @Override
    public VeriBlockMessages.GetVeriBlockPublicationsReply getContent() {
        return veriBlockPublicationsReply;
    }

    public GetVeriBlockPublicationsReplyStreamEvent(
        Peer producer,
        String messageId,
        boolean acknowledgeRequested,
        VeriBlockMessages.GetVeriBlockPublicationsReply veriBlockPublicationsReply
    ) {
        super(producer, messageId, acknowledgeRequested);
        this.veriBlockPublicationsReply = veriBlockPublicationsReply;
    }
}
