// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class GetTransactionRequestStreamEvent extends StreamEvent<VeriBlockMessages.GetTransactionRequest> {
    private final VeriBlockMessages.GetTransactionRequest transactionRequest;

    @Override
    public VeriBlockMessages.GetTransactionRequest getContent() {
        return transactionRequest;
    }

    public GetTransactionRequestStreamEvent(
        Peer producer,
        String messageId,
        boolean acknowledgeRequested,
        VeriBlockMessages.GetTransactionRequest transactionRequest
    ) {
        super(producer, messageId, acknowledgeRequested);
        this.transactionRequest = transactionRequest;
    }
}
