// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class ClearFilterStreamEvent extends StreamEvent<VeriBlockMessages.ClearFilter> {
    private final VeriBlockMessages.ClearFilter clearFilter;

    @Override
    public VeriBlockMessages.ClearFilter getContent() {
        return clearFilter;
    }

    public ClearFilterStreamEvent(Peer producer,
                                String messageId,
                                boolean acknowledgeRequested,
                                VeriBlockMessages.ClearFilter clearFilter) {
        super(producer, messageId, acknowledgeRequested);
        this.clearFilter = clearFilter;
    }
}
