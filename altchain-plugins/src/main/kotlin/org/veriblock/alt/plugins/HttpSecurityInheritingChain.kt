package org.veriblock.alt.plugins

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import org.veriblock.alt.plugins.util.JsonRpcRequestBody
import org.veriblock.alt.plugins.util.RpcResponse
import org.veriblock.alt.plugins.util.handle
import org.veriblock.alt.plugins.util.toJson
import org.veriblock.sdk.alt.SecurityInheritingChain
import java.io.File
import java.time.LocalDateTime

interface HttpSecurityInheritingChain : SecurityInheritingChain {
    val httpClient: HttpClient
    val enableRequestLogging: Boolean
}

suspend inline fun <reified T> HttpSecurityInheritingChain.rpcRequest(method: String, params: Any? = emptyList<Any>()): T {
    val jsonBody = JsonRpcRequestBody(method, params).toJson()
    if (enableRequestLogging) {
        File("./logs/http-calls.log").appendText("${LocalDateTime.now()} -> $jsonBody\n")
    }
    val response: RpcResponse = httpClient.post(config.host) {
        body = jsonBody
    }
    if (enableRequestLogging) {
        File("./logs/http-calls.log").appendText("${LocalDateTime.now()} <- ${response.toJson()}\n")
    }
    return response.handle()
}
