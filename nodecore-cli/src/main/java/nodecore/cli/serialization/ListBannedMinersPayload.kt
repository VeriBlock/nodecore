// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages.ListBannedMinersReply

class ListBannedMinersPayload(
    reply: ListBannedMinersReply
) {
    @SerializedName("banned_clients")
    val bannedClients = reply.clientsList
}
