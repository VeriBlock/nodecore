// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.util

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.reflect.TypeToken
import org.veriblock.alt.plugins.HttpException
import org.veriblock.alt.plugins.fromJson
import java.lang.reflect.Type

data class JsonRpcRequestBody(
    val method: String,
    val params: Any? = emptyList<Any>(),
    val jsonRpc: String = "1.0",
    val id: Int = 1
)

data class RpcResponse(
    val result: JsonElement,
    val error: RpcError?
)

data class RpcError(
    val code: Int,
    val message: String
)

class RpcException(
    val errorCode: Int,
    override val message: String
) : RuntimeException()

class NullResultException(
    override val message: String
) : RuntimeException()

private val gson = Gson()

inline fun <reified T> RpcResponse.handle(requestDescriptor: String): T = try {
    val type: Type = object : TypeToken<T>() {}.type
    when {
        error != null ->
            throw RpcException(error.code, error.message)
        result !is JsonNull
            && result.toString() != "\"0x\""
            && result.toString() != "false" ->
            result.fromJson<T>(type)
        else ->
            throw NullResultException("Null response was returned for request: $requestDescriptor")
    }
} catch (e: RpcException) {
    throw e
} catch (e: NullResultException) {
    throw e
} catch (e: Exception) {
    throw HttpException("Failed to perform request to the API: ${e.message}", e)
}

fun JsonRpcRequestBody.toJson(): String = gson.toJson(this)
fun RpcResponse.toJson(): String = gson.toJson(this)
