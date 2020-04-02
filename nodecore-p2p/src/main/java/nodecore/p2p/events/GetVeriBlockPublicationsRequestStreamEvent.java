// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class GetVeriBlockPublicationsRequestStreamEvent extends StreamEvent<VeriBlockMessages.GetVeriBlockPublicationsRequest> {
    private final VeriBlockMessages.GetVeriBlockPublicationsRequest veriBlockPublicationsRequest;

    @Override
    public VeriBlockMessages.GetVeriBlockPublicationsRequest getContent() {
        return veriBlockPublicationsRequest;
    }

    public GetVeriBlockPublicationsRequestStreamEvent(
        Peer producer,
        String messageId,
        boolean acknowledgeRequested,
        VeriBlockMessages.GetVeriBlockPublicationsRequest veriBlockPublicationsRequest
    ) {
        super(producer, messageId, acknowledgeRequested);
        this.veriBlockPublicationsRequest = veriBlockPublicationsRequest;
    }
}
