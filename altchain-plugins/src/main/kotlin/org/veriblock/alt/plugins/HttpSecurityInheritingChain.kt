package org.veriblock.alt.plugins

import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import java.lang.reflect.Type
import mu.KLogger
import org.veriblock.alt.plugins.util.JsonRpcRequestBody
import org.veriblock.alt.plugins.util.NullResultException
import org.veriblock.alt.plugins.util.RpcResponse
import org.veriblock.alt.plugins.util.handle
import org.veriblock.alt.plugins.util.toJson
import org.veriblock.sdk.alt.SecurityInheritingChain

interface HttpSecurityInheritingChain : SecurityInheritingChain {
    val httpClient: HttpClient
    val requestsLogger: KLogger?
}

suspend inline fun <reified T> HttpSecurityInheritingChain.rpcRequest(
    method: String,
    params: Any? = emptyList<Any>(),
    version: String = "1.0"
): T {
    val jsonBody = JsonRpcRequestBody(method, params, version).toJson()
    requestsLogger?.info { "-> ${jsonBody.take(10_000)}" }
    val rawResponse: String = httpClient.post(config.host) {
        // Since jsonBody is a string, we have to specify it is Json content type
        body = TextContent(jsonBody, contentType = ContentType.Application.Json)
    }
    requestsLogger?.info { "<- ${rawResponse.take(10_000)}" }
    val response = try {
        val type: Type = object : TypeToken<RpcResponse>() {}.type
        rawResponse.fromJson<RpcResponse>(type)
    } catch (e: Exception) {
        error("Unable to parse Altchain (${config.host}) Rpc Response from json: $rawResponse")
    }
    return response.handle(jsonBody)
}

suspend inline fun <reified T> HttpSecurityInheritingChain.nullableRpcRequest(method: String, params: Any? = emptyList<Any>(), version: String = "1.0"): T? = try {
    rpcRequest(method, params, version)
} catch (e: NullResultException) {
    null
}
