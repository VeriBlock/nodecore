// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.p2p.events;

import nodecore.p2p.Peer;

public class PeerMisbehaviorEvent {
    public enum Reason {
        MALFORMED_EVENT,
        UNANNOUNCED,
        UNREQUESTED_BLOCK,
        UNREQUESTED_TRANSACTION,
        VERSION_MISMATCH,
        INVALID_BLOCK,
        INVALID_TRANSACTION,
        ADVERTISEMENT_SIZE,
        MESSAGE_SIZE,
        MESSAGE_SIZE_EXCESSIVE,
        UNFULFILLED_REQUEST_LIMIT,
        UNKNOWN_BLOCK_REQUESTED,
        FREQUENT_KEYSTONE_QUERY
    }

    private final Peer peer;
    public Peer getPeer() {
        return peer;
    }

    private final Reason reason;
    public Reason getReason() {
        return reason;
    }

    public PeerMisbehaviorEvent(Peer peer, Reason reason) {
        this.peer = peer;
        this.reason = reason;
    }
}
