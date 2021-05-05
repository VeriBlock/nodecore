// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.p2p

import io.ktor.util.network.NetworkAddress
import io.ktor.util.network.hostname
import io.ktor.util.network.port
import nodecore.api.grpc.RpcEndpoint
import java.net.InetSocketAddress

object NetworkAddressUtil {
    @JvmStatic
    fun createNetworkAddress(hostname: String, port: Int) =
        NetworkAddress(hostname, port)

    @JvmStatic
    fun createRpcEndpoint(networkAddress: NetworkAddress) =
        RpcEndpoint
            .newBuilder()
            .setAddress(networkAddress.address)
            .setPort(networkAddress.port)
            .build()
}

fun String.asNetworkAddress(): NetworkAddress {
    val parts = split(":")
    return NetworkAddress(
        parts[0],
        parts[1].toInt()
    )
}

val NetworkAddress.address
    get() = (this as? InetSocketAddress)?.address?.hostAddress
        ?: hostname.substringAfter("/")

val NetworkAddress.domain
    get() = hostname.substringBefore("/")

val NetworkAddress.addressKey
    get() = "$address:$port"
