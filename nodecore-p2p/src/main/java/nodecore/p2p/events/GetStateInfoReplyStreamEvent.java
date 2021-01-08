// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class GetStateInfoReplyStreamEvent extends StreamEvent<VeriBlockMessages.GetStateInfoReply> {
    private final VeriBlockMessages.GetStateInfoReply reply;

    @Override
    public VeriBlockMessages.GetStateInfoReply getContent() {
        return reply;
    }

    public GetStateInfoReplyStreamEvent(
        Peer producer,
        String messageId,
        boolean acknowledgeRequested,
        VeriBlockMessages.GetStateInfoReply reply
    ) {
        super(producer, messageId, acknowledgeRequested);
        this.reply = reply;
    }
}
