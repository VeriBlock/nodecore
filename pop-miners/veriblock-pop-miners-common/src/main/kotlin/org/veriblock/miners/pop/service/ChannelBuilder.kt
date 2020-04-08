// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service

import io.grpc.Channel
import io.grpc.ClientInterceptors
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.MetadataUtils
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.Constants
import org.veriblock.miners.pop.common.NodeCoreRpcConfig
import java.io.File

private val logger = createLogger {}

class ChannelBuilder(
    private val config: NodeCoreRpcConfig
) {
    fun buildManagedChannel(): ManagedChannel {
        logger.info(
            "Connecting to NodeCore at {}:{} {}", config.host, config.port,
            if (config.ssl) "over SSL" else ""
        )
        return if (config.ssl) {
            buildTlsManagedChannel()
        } else {
            buildPlainTextManagedChannel()
        }
    }

    fun attachPasswordInterceptor(inner: Channel?): Channel {
        val headers = Metadata()
        val password = config.password
        if (password != null) {
            headers.put(Constants.RPC_PASSWORD_HEADER_NAME, password)
        }
        val clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(headers)
        return ClientInterceptors.intercept(inner, clientInterceptor)
    }

    private fun buildTlsManagedChannel(): ManagedChannel {
        if (config.certificateChainPath == null) {
            error("'nodecore.rpc.certificateChainPath' is not configured")
        }
        val certChainFile = File(config.certificateChainPath)
        return NettyChannelBuilder
            .forAddress(config.host, config.port)
            .sslContext(GrpcSslContexts.forClient().trustManager(certChainFile).build())
            .negotiationType(NegotiationType.TLS)
            .build()
    }

    private fun buildPlainTextManagedChannel(): ManagedChannel {
        return NettyChannelBuilder
            .forAddress(config.host, config.port)
            .usePlaintext()
            .build()
    }
}
