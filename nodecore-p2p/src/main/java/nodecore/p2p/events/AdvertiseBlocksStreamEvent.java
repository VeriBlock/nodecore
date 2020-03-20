// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class AdvertiseBlocksStreamEvent extends StreamEvent<VeriBlockMessages.AdvertiseBlocks> {
    private final VeriBlockMessages.AdvertiseBlocks advertiseBlocks;

    @Override
    public VeriBlockMessages.AdvertiseBlocks getContent() {
        return advertiseBlocks;
    }

    public AdvertiseBlocksStreamEvent(Peer producer,
                                      String messageId,
                                      boolean acknowledgeRequested,
                                      VeriBlockMessages.AdvertiseBlocks advertiseBlocks) {
        super(producer, messageId, acknowledgeRequested);
        this.advertiseBlocks = advertiseBlocks;
    }
}
