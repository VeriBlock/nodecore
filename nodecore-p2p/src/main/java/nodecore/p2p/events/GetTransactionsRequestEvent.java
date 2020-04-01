// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class GetTransactionsRequestEvent extends StreamEvent<VeriBlockMessages.GetTransactionsRequest> {
    private final VeriBlockMessages.GetTransactionsRequest transactionsRequest;

    @Override
    public VeriBlockMessages.GetTransactionsRequest getContent() {
        return transactionsRequest;
    }

    public GetTransactionsRequestEvent(
        Peer producer,
        String messageId,
        boolean acknowledgeRequested,
        VeriBlockMessages.GetTransactionsRequest transactionRequest
    ) {
        super(producer, messageId, acknowledgeRequested);
        this.transactionsRequest = transactionRequest;
    }
}
