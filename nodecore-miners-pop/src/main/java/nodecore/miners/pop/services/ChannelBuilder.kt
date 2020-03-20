// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.miners.pop.services

import io.grpc.Channel
import io.grpc.ClientInterceptors
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.MetadataUtils
import nodecore.miners.pop.Configuration
import nodecore.miners.pop.Constants
import java.io.File
import javax.net.ssl.SSLException

class ChannelBuilder(
    private val configuration: Configuration
) {
    @Throws(SSLException::class)
    fun buildManagedChannel(): ManagedChannel {
        return if (configuration.nodeCoreUseSSL) {
            buildTlsManagedChannel()
        } else {
            buildPlainTextManagedChannel()
        }
    }

    fun attachPasswordInterceptor(inner: Channel?): Channel {
        val headers = Metadata()
        val password = configuration.nodeCorePassword
        if (password != null) {
            headers.put(Constants.RPC_PASSWORD_HEADER_NAME, password)
        }
        val clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(headers)
        return ClientInterceptors.intercept(inner, clientInterceptor)
    }

    private fun buildTlsManagedChannel(): ManagedChannel {
        val certChainFile = File(configuration.certificateChainPath)
        return NettyChannelBuilder
            .forAddress(configuration.nodeCoreHost, configuration.nodeCorePort)
            .sslContext(GrpcSslContexts.forClient().trustManager(certChainFile).build())
            .negotiationType(NegotiationType.TLS)
            .build()
    }

    private fun buildPlainTextManagedChannel(): ManagedChannel {
        return NettyChannelBuilder
            .forAddress(configuration.nodeCoreHost, configuration.nodeCorePort)
            .usePlaintext()
            .build()
    }
}
