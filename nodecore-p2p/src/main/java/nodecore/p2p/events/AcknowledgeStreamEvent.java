// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class AcknowledgeStreamEvent extends StreamEvent<VeriBlockMessages.Acknowledgement> {
    private final VeriBlockMessages.Acknowledgement acknowledgement;

    @Override
    public VeriBlockMessages.Acknowledgement getContent() {
        return acknowledgement;
    }

    public AcknowledgeStreamEvent(Peer producer,
                                  String messageId,
                                  boolean acknowledgeRequested,
                                  VeriBlockMessages.Acknowledgement acknowledgement) {
        super(producer, messageId, acknowledgeRequested);
        this.acknowledgement = acknowledgement;
    }
}
