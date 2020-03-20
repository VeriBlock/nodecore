// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class AnnounceStreamEvent extends StreamEvent<VeriBlockMessages.Announce> {
    private final VeriBlockMessages.Announce announce;

    @Override
    public VeriBlockMessages.Announce getContent() {
        return announce;
    }

    public AnnounceStreamEvent(Peer producer,
                               String messageId,
                               boolean acknowledgeRequested,
                               VeriBlockMessages.Announce announce) {
        super(producer, messageId, acknowledgeRequested);
        this.announce = announce;
    }
}
