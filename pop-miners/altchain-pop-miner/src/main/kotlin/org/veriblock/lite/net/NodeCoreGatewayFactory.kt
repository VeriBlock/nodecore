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
import org.veriblock.core.utilities.createLogger
import org.veriblock.lite.net.impl.GatewayStrategyGrpcImpl
import org.veriblock.lite.net.impl.GatewayStrategySpvImpl
import org.veriblock.lite.params.NetworkParameters
import veriblock.SpvContext
import veriblock.model.DownloadStatusResponse
import veriblock.net.BootstrapPeerDiscovery

private val logger = createLogger {}

object NodeCoreGatewayFactory {

    fun create(networkParameters: NetworkParameters): GatewayStrategy {

        if (!networkParameters.isSpv) {
            val rpcConfiguration = configure(networkParameters)
            val channelBuilder = ChannelBuilder(rpcConfiguration)
            val channel = channelBuilder.buildManagedChannel()
            val blockingStub = AdminGrpc.newBlockingStub(channelBuilder.attachPasswordInterceptor(channel))
                .withMaxInboundMessageSize(20 * 1024 * 1024)
                .withMaxOutboundMessageSize(20 * 1024 * 1024)

            return GatewayStrategyGrpcImpl(blockingStub, channel)
        } else {
            val spvContext = SpvContext()
            spvContext.init(
                networkParameters.spvNetworkParameters,
                BootstrapPeerDiscovery(networkParameters.spvNetworkParameters), false
            )
            spvContext.peerTable.start()

            logger.info { "Initialize SPV: " }
            while (true) {
                val status: DownloadStatusResponse = spvContext.peerTable.downloadStatus
                if (status.downloadStatus.isDiscovering) {
                    logger.info { "Waiting for peers response." }
                } else if (status.downloadStatus.isDownloading) {
                    logger.info { "Blockchain is downloading. " + status.currentHeight + " / " + status.bestHeight }
                } else {
                    logger.info { "Blockchain is ready. Current height " + status.currentHeight }
                    break
                }
                Thread.sleep(5000L)
            }

            return GatewayStrategySpvImpl(spvContext)
        }

    }

    private fun configure(networkParameters: NetworkParameters): AdminRpcConfiguration = AdminRpcConfiguration().apply {
        isSsl = networkParameters.isSsl
        certificateChainPath = networkParameters.certificateChainPath
        nodeCoreHost = networkParameters.adminHost
        nodeCorePort = networkParameters.adminPort
        nodeCorePassword = networkParameters.adminPassword
    }
}
