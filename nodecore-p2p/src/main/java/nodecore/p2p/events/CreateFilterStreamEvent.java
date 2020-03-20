// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class CreateFilterStreamEvent extends StreamEvent<VeriBlockMessages.CreateFilter> {
    private final VeriBlockMessages.CreateFilter createFilter;

    @Override
    public VeriBlockMessages.CreateFilter getContent() {
        return createFilter;
    }

    public CreateFilterStreamEvent(Peer producer,
                               String messageId,
                               boolean acknowledgeRequested,
                               VeriBlockMessages.CreateFilter createFilter) {
        super(producer, messageId, acknowledgeRequested);
        this.createFilter = createFilter;
    }
}
