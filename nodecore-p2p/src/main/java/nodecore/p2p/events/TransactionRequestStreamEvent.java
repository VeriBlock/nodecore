// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class TransactionRequestStreamEvent extends StreamEvent<VeriBlockMessages.TransactionRequest> {
    private final VeriBlockMessages.TransactionRequest transactionRequest;

    @Override
    public VeriBlockMessages.TransactionRequest getContent() {
        return transactionRequest;
    }

    public TransactionRequestStreamEvent(Peer producer,
                                         String messageId,
                                         boolean acknowledgeRequested,
                                         VeriBlockMessages.TransactionRequest transactionRequest) {
        super(producer, messageId, acknowledgeRequested);
        this.transactionRequest = transactionRequest;
    }
}
