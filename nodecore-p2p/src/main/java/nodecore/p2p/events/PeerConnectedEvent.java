// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.p2p.Peer;

public class PeerConnectedEvent {
    private final Peer peer;

    public Peer getPeer() {
        return peer;
    }

    public PeerConnectedEvent(Peer peer) {
        this.peer = peer;
    }
}
