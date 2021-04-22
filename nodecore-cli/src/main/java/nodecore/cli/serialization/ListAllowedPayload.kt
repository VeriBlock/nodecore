// VeriBlock NodeCore CLI
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import nodecore.api.grpc.RpcListAllowedReply

class ListAllowedPayload(
    reply: RpcListAllowedReply
) {
    val allowed = reply.entriesList.map { whiteListInfo  ->
        WhitelistInfo(whiteListInfo)
    }
}
