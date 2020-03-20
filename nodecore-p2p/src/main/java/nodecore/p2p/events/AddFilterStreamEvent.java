// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class AddFilterStreamEvent extends StreamEvent<VeriBlockMessages.AddFilter> {
    private final VeriBlockMessages.AddFilter addFilter;

    @Override
    public VeriBlockMessages.AddFilter getContent() {
        return addFilter;
    }

    public AddFilterStreamEvent(Peer producer,
                                   String messageId,
                                   boolean acknowledgeRequested,
                                   VeriBlockMessages.AddFilter addFilter) {
        super(producer, messageId, acknowledgeRequested);
        this.addFilter = addFilter;
    }
}
