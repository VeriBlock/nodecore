// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class NotFoundStreamEvent extends StreamEvent<VeriBlockMessages.NotFound> {
    private final VeriBlockMessages.NotFound notFound;

    @Override
    public VeriBlockMessages.NotFound getContent() {
        return notFound;
    }

    public NotFoundStreamEvent(Peer producer,
                               String messageId,
                               boolean acknowledgeRequested,
                               VeriBlockMessages.NotFound notFound) {
        super(producer, messageId, acknowledgeRequested);
        this.notFound = notFound;
    }
}
