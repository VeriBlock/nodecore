package org.veriblock.alt.plugins

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.RollingPolicy
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import mu.KLogger
import org.slf4j.LoggerFactory
import org.veriblock.alt.plugins.util.JsonRpcRequestBody
import org.veriblock.alt.plugins.util.RpcResponse
import org.veriblock.alt.plugins.util.handle
import org.veriblock.alt.plugins.util.toJson
import org.veriblock.sdk.alt.SecurityInheritingChain
import java.io.File
import java.time.LocalDateTime

interface HttpSecurityInheritingChain : SecurityInheritingChain {
    val httpClient: HttpClient
    val requestsLogger: KLogger?
}

suspend inline fun <reified T> HttpSecurityInheritingChain.rpcRequest(method: String, params: Any? = emptyList<Any>()): T {
    val jsonBody = JsonRpcRequestBody(method, params).toJson()
    requestsLogger?.info { "-> $jsonBody" }

    val response: RpcResponse = httpClient.post(config.host) {
        body = jsonBody
    }
    requestsLogger?.info { "<- ${response.toJson()}" }

    return response.handle()
}
