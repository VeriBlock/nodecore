package veriblock.service.impl;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class AdminServerInterceptor  implements ServerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AdminServerInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        InetSocketAddress socketAddress = (InetSocketAddress) call
                .getAttributes()
                .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);

        if (socketAddress != null) {
            logger.info("RPS Admin, Request:" + call.getMethodDescriptor().getFullMethodName());
        }

        return Contexts.interceptCall(
                Context.current(),
                call,
                headers,
                next);
    }
}
