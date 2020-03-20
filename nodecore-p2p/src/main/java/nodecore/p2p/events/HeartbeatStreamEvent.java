// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class HeartbeatStreamEvent extends StreamEvent<VeriBlockMessages.Heartbeat> {
    private final VeriBlockMessages.Heartbeat heartbeat;

    @Override
    public VeriBlockMessages.Heartbeat getContent() {
        return heartbeat;
    }

    public HeartbeatStreamEvent(Peer producer,
                                String messageId,
                                boolean acknowledgeRequested,
                                VeriBlockMessages.Heartbeat heartbeat) {
        super(producer, messageId, acknowledgeRequested);
        this.heartbeat = heartbeat;
    }
}
