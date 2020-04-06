// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class GetDebugVTBsReplyStreamEvent extends StreamEvent<VeriBlockMessages.GetDebugVTBsReply> {
    private final VeriBlockMessages.GetDebugVTBsReply debugVTBsReply;

    @Override
    public VeriBlockMessages.GetDebugVTBsReply getContent() {
        return debugVTBsReply;
    }

    public GetDebugVTBsReplyStreamEvent(
        Peer producer,
        String messageId,
        boolean acknowledgeRequested,
        VeriBlockMessages.GetDebugVTBsReply getDebugVTBsReply
    ) {
        super(producer, messageId, acknowledgeRequested);
        this.debugVTBsReply = getDebugVTBsReply;
    }
}
