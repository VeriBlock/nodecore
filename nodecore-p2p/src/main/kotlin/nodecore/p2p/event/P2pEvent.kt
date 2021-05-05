// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p.event

import nodecore.api.grpc.RpcEventOrBuilder
import nodecore.p2p.Peer

class P2pEvent<T>(
    val producer: Peer,
    val messageId: String,
    val acknowledgeRequested: Boolean,
    val content: T
)

inline fun <reified T> RpcEventOrBuilder.toP2pEvent(peer: Peer, content: T) =
    P2pEvent(peer, id, acknowledge, content)
