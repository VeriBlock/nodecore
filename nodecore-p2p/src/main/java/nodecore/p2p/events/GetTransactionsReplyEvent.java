// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class GetTransactionsReplyEvent extends StreamEvent<VeriBlockMessages.GetTransactionsReply> {
    private final VeriBlockMessages.GetTransactionsReply transactionsReply;

    @Override
    public VeriBlockMessages.GetTransactionsReply getContent() {
        return transactionsReply;
    }

    public GetTransactionsReplyEvent(
        Peer producer,
        String messageId,
        boolean acknowledgeRequested,
        VeriBlockMessages.GetTransactionsReply transactionReply
    ) {
        super(producer, messageId, acknowledgeRequested);
        this.transactionsReply = transactionReply;
    }
}
