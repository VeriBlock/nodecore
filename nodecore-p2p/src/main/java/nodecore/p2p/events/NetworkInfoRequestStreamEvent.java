// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class NetworkInfoRequestStreamEvent extends StreamEvent<VeriBlockMessages.NetworkInfoRequest> {
    private final VeriBlockMessages.NetworkInfoRequest networkInfoRequest;

    @Override
    public VeriBlockMessages.NetworkInfoRequest getContent() {
        return networkInfoRequest;
    }

    public NetworkInfoRequestStreamEvent(Peer producer,
                                         String messageId,
                                         boolean acknowledgeRequested,
                                         VeriBlockMessages.NetworkInfoRequest networkInfoRequest) {
        super(producer, messageId, acknowledgeRequested);
        this.networkInfoRequest = networkInfoRequest;
    }
}
