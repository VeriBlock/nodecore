// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli.serialization

import com.google.gson.annotations.SerializedName
import nodecore.api.grpc.VeriBlockMessages

class NodeInfo(
    info: VeriBlockMessages.NodeInfo
) {
    val address = info.address

    val port = info.port

    val application = info.application

    @SerializedName("protocol_version")
    val protocolVersion = info.protocolVersion

    val platform = info.platform

    @SerializedName("start_timestamp")
    val startTimestamp = info.startTimestamp
}
