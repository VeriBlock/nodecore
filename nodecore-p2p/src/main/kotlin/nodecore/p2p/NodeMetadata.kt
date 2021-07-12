// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import nodecore.api.grpc.RpcNodeInfo

data class NodeMetadata(
    val address: String = "",
    val port: Int = 0,
    val application: String = "Unknown",
    val protocolVersion: Int = 0,
    val platform: String = "Unknown",
    val startTimestamp: Int = 0,
    val canShareAddress: Boolean = false,
    val capabilities: PeerCapabilities = PeerCapabilities.parse(0),
    val id: String = ""
) {
    val addressKey: String
        get() = "$address:$port"

    // FIXME: Convert to extension function when it's no longer called from java code
    fun toRpcNodeInfo(addr: String? = null): RpcNodeInfo = RpcNodeInfo.newBuilder().also {
        it.address = addr ?: address
        it.port = port
        it.application = application
        it.protocolVersion = protocolVersion
        it.platform = platform
        it.startTimestamp = startTimestamp
        it.share = canShareAddress
        it.capabilities = capabilities.toBitVector()
        it.id = id
    }.build()
}

fun RpcNodeInfo.toModel() = NodeMetadata(
    address,
    port,
    application,
    protocolVersion,
    platform,
    startTimestamp,
    share,
    PeerCapabilities.parse(capabilities),
    id
)
