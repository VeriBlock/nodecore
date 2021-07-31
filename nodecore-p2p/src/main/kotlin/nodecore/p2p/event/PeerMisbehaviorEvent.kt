// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p.event

import nodecore.p2p.Peer

data class PeerMisbehaviorEvent(
    val peer: Peer,
    val reason: Reason,
    val message: String
) {
    enum class Reason {
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
}
