// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class BlockQueryReplyStreamEvent extends StreamEvent<VeriBlockMessages.BlockQueryReply> {
    private final VeriBlockMessages.BlockQueryReply queryReply;

    @Override
    public VeriBlockMessages.BlockQueryReply getContent() {
        return queryReply;
    }

    public BlockQueryReplyStreamEvent(Peer producer,
                                      String messageId,
                                      boolean acknowledgeRequested,
                                      VeriBlockMessages.BlockQueryReply queryReply) {
        super(producer, messageId, acknowledgeRequested);
        this.queryReply = queryReply;
    }
}
