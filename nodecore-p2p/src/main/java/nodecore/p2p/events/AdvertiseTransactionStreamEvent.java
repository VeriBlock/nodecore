// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class AdvertiseTransactionStreamEvent extends StreamEvent<VeriBlockMessages.AdvertiseTransaction> {
    private final VeriBlockMessages.AdvertiseTransaction advertiseTransaction;

    @Override
    public VeriBlockMessages.AdvertiseTransaction getContent() {
        return advertiseTransaction;
    }

    public AdvertiseTransactionStreamEvent(Peer producer,
                                           String messageId,
                                           boolean acknowledgeRequested,
                                           VeriBlockMessages.AdvertiseTransaction advertiseTransaction) {
        super(producer, messageId, acknowledgeRequested);
        this.advertiseTransaction = advertiseTransaction;
    }
}
