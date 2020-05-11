package veriblock.service

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Grpc
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.slf4j.LoggerFactory
import org.veriblock.core.utilities.createLogger
import java.net.InetSocketAddress

private val logger = createLogger {}

class AdminServerInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val socketAddress = call
            .attributes
            .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR) as InetSocketAddress?
        if (socketAddress != null) {
            logger.info("RPS Admin, Request:" + call.methodDescriptor.fullMethodName)
        }
        return Contexts.interceptCall(
            Context.current(),
            call,
            headers,
            next
        )
    }
}
