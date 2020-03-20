// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.grpc.utilities;

import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import nodecore.api.grpc.AdminRpcConfiguration;
import nodecore.api.grpc.Constants;

import javax.net.ssl.SSLException;
import java.io.File;

public class ChannelBuilder {
    private final AdminRpcConfiguration configuration;

    public ChannelBuilder(AdminRpcConfiguration configuration) {
        this.configuration = configuration;
    }

    public ManagedChannel buildManagedChannel() throws SSLException {
        if (configuration.isSsl()) {
            return buildTlsManagedChannel();
        }

        return buildPlainTextManagedChannel();
    }

    public Channel attachPasswordInterceptor(Channel inner) {
        String password = configuration.getNodeCorePassword();
        Metadata headers = new Metadata();
        if(password != null) {
            headers.put(Constants.RPC_PASSWORD_HEADER_NAME, password);
        }
        ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(headers);
        return ClientInterceptors.intercept(inner, clientInterceptor);
    }

    private ManagedChannel buildTlsManagedChannel() throws SSLException {
        File certChainFile = new File(configuration.getCertificateChainPath());
        return NettyChannelBuilder.forAddress(configuration.getNodeCoreHost(), configuration.getNodeCorePort())
                .sslContext(GrpcSslContexts.forClient().trustManager(certChainFile).build())
                .negotiationType(NegotiationType.TLS)
                .build();
    }

    private ManagedChannel buildPlainTextManagedChannel() {
        return NettyChannelBuilder.forAddress(configuration.getNodeCoreHost(), configuration.getNodeCorePort())
                .usePlaintext()
                .build();
    }
}
