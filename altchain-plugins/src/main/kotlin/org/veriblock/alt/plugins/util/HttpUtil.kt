// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins.util

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.veriblock.core.utilities.createLogger
import java.lang.reflect.Type

class HttpException(
    val responseStatusCode: Int,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

private val gson = Gson()

val httpLogger = createLogger {}

inline fun <reified T : Any> Request.httpResponse(): T = try {
    val (_, response, result) = response()
    val responseBody = response.body().asString("application/json")
    httpLogger.debug { "Request call: ${cUrlString()}" }
    httpLogger.debug { "Response Body: ${responseBody.trim()}" }
    if (result is Result.Failure) {
        throw HttpException(response.statusCode, "Call to API failed! Cause: ${result.error.message}; Response body: $responseBody", result.error)
    }
    responseBody.fromJson(object : TypeToken<T>() {}.type)
} catch (e: FuelError) {
    throw HttpException(-1, "Failed to perform request to the API: ${e.message}", e)
}

fun <T> String.fromJson(type: Type): T = gson.fromJson(this, type)
fun <T> JsonElement.fromJson(type: Type): T = gson.fromJson(this, type)
