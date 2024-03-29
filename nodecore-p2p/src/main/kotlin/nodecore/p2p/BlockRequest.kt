// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import nodecore.api.grpc.RpcBlockHeader
import nodecore.p2p.PeerRequest

class BlockRequest(
    val hash: String,
    val header: RpcBlockHeader,
    override val peer: Peer
) : PeerRequest {
    override var requestedAt = 0
}
