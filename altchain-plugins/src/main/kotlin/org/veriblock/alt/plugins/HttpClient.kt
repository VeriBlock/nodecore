// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins

import com.google.gson.ExclusionStrategy
import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.Json
import io.ktor.http.ContentType
import org.veriblock.core.crypto.MerkleRoot
import org.veriblock.core.crypto.PreviousBlockVbkHash
import org.veriblock.core.crypto.PreviousKeystoneVbkHash
import org.veriblock.core.crypto.TruncatedMerkleRoot
import org.veriblock.core.crypto.VbkHash
import org.veriblock.core.crypto.asMerkleRoot
import org.veriblock.core.crypto.asTruncatedMerkleRoot
import org.veriblock.core.crypto.asVbkHash
import org.veriblock.core.crypto.asVbkPreviousBlockHash
import org.veriblock.core.crypto.asVbkPreviousKeystoneHash
import org.veriblock.sdk.alt.plugin.HttpAuthConfig
import java.lang.reflect.Type

class HttpException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

private val gson = Gson()
    .newBuilder()
    .registerTypeAdapter(PreviousBlockVbkHash::class.java, JsonDeserializer { json, _, _ -> json.asString.asVbkPreviousBlockHash() })
    .registerTypeAdapter(PreviousKeystoneVbkHash::class.java, JsonDeserializer { json, _, _ -> json.asString.asVbkPreviousKeystoneHash() })
    .registerTypeAdapter(VbkHash::class.java, JsonDeserializer { json, _, _ -> json.asString.asVbkHash() })
    .registerTypeAdapter(MerkleRoot::class.java, JsonDeserializer { json, _, _ -> json.asString.asMerkleRoot() })
    .registerTypeAdapter(TruncatedMerkleRoot::class.java, JsonDeserializer { json, _, _ -> json.asString.asTruncatedMerkleRoot() })
    .create()

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
                credentials {
                    BasicAuthCredentials(authConfig.username, authConfig.password)
                }
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
