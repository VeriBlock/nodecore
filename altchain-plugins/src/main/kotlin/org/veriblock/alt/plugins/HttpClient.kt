// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins

import com.google.gson.Gson
import com.google.gson.JsonElement
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.basic
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.Json
import io.ktor.http.ContentType
import org.veriblock.sdk.alt.plugin.HttpAuthConfig
import java.lang.reflect.Type

class HttpException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

private val gson = Gson()

fun <T> String.fromJson(type: Type): T = gson.fromJson(this, type)
fun <T> JsonElement.fromJson(type: Type): T = gson.fromJson(this, type)

fun createHttpClient(
    authConfig: HttpAuthConfig? = null,
    contentTypes: List<ContentType>? = null,
    connectionTimeout: Int = 10_000
) = HttpClient(Apache) {
    Json {
        serializer = GsonSerializer()
        if (contentTypes != null) {
            acceptContentTypes = contentTypes
        }
    }
    if (authConfig != null) {
        Auth {
            basic {
                username = authConfig.username
                password = authConfig.password
            }
        }
    }
    engine {
        socketTimeout = connectionTimeout
        connectTimeout = connectionTimeout
        connectionRequestTimeout = connectionTimeout * 2
    }
    // We will handle error responses manually as we'll be calling a RPC service's API
    expectSuccess = false
}
