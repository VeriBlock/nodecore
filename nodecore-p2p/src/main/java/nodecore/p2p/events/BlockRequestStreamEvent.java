// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class BlockRequestStreamEvent extends StreamEvent<VeriBlockMessages.BlockRequest> {
    private final VeriBlockMessages.BlockRequest blockRequest;

    @Override
    public VeriBlockMessages.BlockRequest getContent() {
        return blockRequest;
    }

    public BlockRequestStreamEvent(Peer producer,
                                   String messageId,
                                   boolean acknowledgeRequested,
                                   VeriBlockMessages.BlockRequest blockRequest) {
        super(producer, messageId, acknowledgeRequested);
        this.blockRequest = blockRequest;
    }
}
