// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class GetDebugVTBsRequestStreamEvent extends StreamEvent<VeriBlockMessages.GetDebugVTBsRequest> {
    private final VeriBlockMessages.GetDebugVTBsRequest debugVTBsRequest;

    @Override
    public VeriBlockMessages.GetDebugVTBsRequest getContent() {
        return debugVTBsRequest;
    }

    public GetDebugVTBsRequestStreamEvent(
        Peer producer,
        String messageId,
        boolean acknowledgeRequested,
        VeriBlockMessages.GetDebugVTBsRequest debugVTBsRequest
    ) {
        super(producer, messageId, acknowledgeRequested);
        this.debugVTBsRequest = debugVTBsRequest;
    }
}
