package org.veriblock.alt.plugins

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.*
import mu.KLogger
import org.veriblock.alt.plugins.util.JsonRpcRequestBody
import org.veriblock.alt.plugins.util.RpcResponse
import org.veriblock.alt.plugins.util.handle
import org.veriblock.alt.plugins.util.toJson
import org.veriblock.sdk.alt.SecurityInheritingChain

interface HttpSecurityInheritingChain : SecurityInheritingChain {
    val httpClient: HttpClient
    val requestsLogger: KLogger?
}

suspend inline fun <reified T> HttpSecurityInheritingChain.rpcRequest(method: String, params: Any? = emptyList<Any>()): T {
    val jsonBody = JsonRpcRequestBody(method = method, params = params).toJson()
    requestsLogger?.info { "-> ${jsonBody.take(10_000)}" }
    val response: RpcResponse = httpClient.post(config.host) {
        contentType(ContentType.Application.Json) // FIXME
        body = jsonBody
    }
    requestsLogger?.info { "<- ${response.toJson().take(10_000)}" }
    return response.handle()
}
