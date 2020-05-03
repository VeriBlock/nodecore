// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.net

import nodecore.api.grpc.AdminGrpc
import nodecore.api.grpc.AdminRpcConfiguration
import nodecore.api.grpc.utilities.ChannelBuilder
import org.veriblock.lite.net.impl.GatewayStrategyGrpcImpl
import org.veriblock.lite.net.impl.GatewayStrategySpvImpl
import org.veriblock.lite.params.NetworkParameters
import veriblock.SpvContext

fun createFullNode(networkParameters: NetworkParameters): GatewayStrategy {
    val rpcConfiguration = configure(networkParameters)
    val channelBuilder = ChannelBuilder(rpcConfiguration)
    val channel = channelBuilder.buildManagedChannel()
    val blockingStub = AdminGrpc.newBlockingStub(channelBuilder.attachPasswordInterceptor(channel))
        .withMaxInboundMessageSize(20 * 1024 * 1024)
        .withMaxOutboundMessageSize(20 * 1024 * 1024)

    return GatewayStrategyGrpcImpl(blockingStub, channel, networkParameters)
}

fun createSpv(spvContext: SpvContext): GatewayStrategy {
    return GatewayStrategySpvImpl(spvContext)
}

private fun configure(networkParameters: NetworkParameters): AdminRpcConfiguration = AdminRpcConfiguration().apply {
    isSsl = networkParameters.isSsl
    certificateChainPath = networkParameters.certificateChainPath
    nodeCoreHost = networkParameters.adminHost
    nodeCorePort = networkParameters.adminPort
    nodeCorePassword = networkParameters.adminPassword
}
