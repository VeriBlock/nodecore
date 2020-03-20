// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class BlockQueryStreamEvent extends StreamEvent<VeriBlockMessages.BlockQuery> {
    private final VeriBlockMessages.BlockQuery blockQuery;

    @Override
    public VeriBlockMessages.BlockQuery getContent() {
        return blockQuery;
    }

    public BlockQueryStreamEvent(Peer producer,
                                 String messageId,
                                 boolean acknowledgeRequested,
                                 VeriBlockMessages.BlockQuery blockQuery) {
        super(producer, messageId, acknowledgeRequested);
        this.blockQuery = blockQuery;
    }
}
