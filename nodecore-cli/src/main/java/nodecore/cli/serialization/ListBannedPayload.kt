// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.ListBannedReply

class ListBannedPayload(
    reply: ListBannedReply
) {
    @SerializedName("banned_peers")
    val bannedPeers = reply.entriesList.map { blackListInfo ->
        BlacklistInfo(blackListInfo)
    }
}
