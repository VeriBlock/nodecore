// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class GetStateInfoRequestStreamEvent extends StreamEvent<VeriBlockMessages.GetStateInfoRequest> {
    private final VeriBlockMessages.GetStateInfoRequest request;

    @Override
    public VeriBlockMessages.GetStateInfoRequest getContent() {
        return request;
    }

    public GetStateInfoRequestStreamEvent(
        Peer producer,
        String messageId,
        boolean acknowledgeRequested,
        VeriBlockMessages.GetStateInfoRequest request
    ) {
        super(producer, messageId, acknowledgeRequested);
        this.request = request;
    }
}
