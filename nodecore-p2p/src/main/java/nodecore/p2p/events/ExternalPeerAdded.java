// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.p2p.PeerEndpoint;

public class ExternalPeerAdded {
    private final PeerEndpoint peerEndpoint;
    public PeerEndpoint getPeerEndpoint() {
        return peerEndpoint;
    }

    public ExternalPeerAdded(PeerEndpoint peerEndpoint) {
        this.peerEndpoint = peerEndpoint;
    }
}
