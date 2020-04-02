// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class GetTransactionReplyStreamEvent extends StreamEvent<VeriBlockMessages.GetTransactionReply> {
    private final VeriBlockMessages.GetTransactionReply transactionReply;

    @Override
    public VeriBlockMessages.GetTransactionReply getContent() {
        return transactionReply;
    }

    public GetTransactionReplyStreamEvent(
        Peer producer,
        String messageId,
        boolean acknowledgeRequested,
        VeriBlockMessages.GetTransactionReply transactionReply
    ) {
        super(producer, messageId, acknowledgeRequested);
        this.transactionReply = transactionReply;
    }
}
