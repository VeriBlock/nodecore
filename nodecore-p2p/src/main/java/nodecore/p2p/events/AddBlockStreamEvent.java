// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.api.grpc.VeriBlockMessages;
import nodecore.p2p.Peer;

public class AddBlockStreamEvent extends StreamEvent<VeriBlockMessages.Block> {
    private final VeriBlockMessages.Block block;

    @Override
    public VeriBlockMessages.Block getContent() {
        return block;
    }

    public AddBlockStreamEvent(Peer producer,
                               String messageId,
                               boolean acknowledgeRequested,
                               VeriBlockMessages.Block block) {
        super(producer, messageId, acknowledgeRequested);
        this.block = block;
    }
}
