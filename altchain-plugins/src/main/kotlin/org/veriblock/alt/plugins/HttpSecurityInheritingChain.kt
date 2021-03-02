package org.veriblock.alt.plugins

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
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

suspend inline fun <reified T> HttpSecurityInheritingChain.rpcRequest(method: String, params: Any? = emptyList<Any>(), version: String = "1.0"): T {
    val jsonBody = JsonRpcRequestBody(method, params, version).toJson()
    requestsLogger?.info { "-> ${jsonBody.take(10_000)}" }
    val response: RpcResponse = httpClient.post(config.host) {
        // Since jsonBody is a string, we have to specify it is Json content type
        body = TextContent(jsonBody, contentType = ContentType.Application.Json)
    }
    requestsLogger?.info { "<- ${response.toJson().take(10_000)}" }
    return response.handle()
}
