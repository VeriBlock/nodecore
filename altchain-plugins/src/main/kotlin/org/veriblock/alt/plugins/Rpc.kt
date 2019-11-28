// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.veriblock.sdk.createLogger
import java.lang.reflect.Type

data class JsonRpcRequestBody(
    val method: String,
    val params: Any? = emptyList<Any>(),
    val jsonRpc: String = "1.0"
)

data class RpcResponse(
    val result: JsonElement?,
    val error: RpcError?
)

data class RpcError(
    val code: Int,
    val message: String
)

class RpcException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

private val gson = Gson()

val rpcLogger = createLogger {}

inline fun <reified T : Any> Request.rpcResponse(): T = try {
    val (_, response, result) = response()
    val responseBody = response.body().asString("application/json")
    rpcLogger.debug { "Request Body: ${this.body.asString("application/json")}" }
    rpcLogger.debug { "Response Body: ${responseBody.trim()}" }
    if (result is Result.Failure) {
        throw RpcException("Call to RPC API failed! Cause: ${result.error.message}; Response body: $responseBody", result.error)
    }
    val type: Type = object : TypeToken<T>() {}.type
    val rpcResponse: RpcResponse = responseBody.fromJson(object : TypeToken<RpcResponse>() {}.type)
    when {
        rpcResponse.result != null ->
            rpcResponse.result.fromJson<T>(type)
        rpcResponse.error != null ->
            throw RpcException(rpcResponse.error.message)
        else ->
            throw IllegalStateException()
    }
} catch (e: FuelError) {
    throw RpcException("Failed to perform request to the API: ${e.message}", e)
}

fun JsonRpcRequestBody.toJson(): String = gson.toJson(this)
