// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class AddTransactionStreamEvent extends StreamEvent<VeriBlockMessages.TransactionUnion> {
    private final VeriBlockMessages.TransactionUnion transaction;

    @Override
    public VeriBlockMessages.TransactionUnion getContent() {
        return transaction;
    }

    public AddTransactionStreamEvent(Peer producer,
                                     String messageId,
                                     boolean acknowledgeRequested,
                                     VeriBlockMessages.TransactionUnion transaction) {
        super(producer, messageId, acknowledgeRequested);
        this.transaction = transaction;
    }
}
