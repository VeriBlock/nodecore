// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.lamba92.ktor.features.SinglePageApplication
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.openAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.schema.namer.DefaultSchemaNamer
import com.papsign.ktor.openapigen.schema.namer.SchemaNamer
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpMethod
import io.ktor.jackson.jackson
import io.ktor.locations.Locations
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.api.auth.BasicProvider
import org.veriblock.miners.pop.api.auth.basicAuth
import org.veriblock.miners.pop.api.auth.installAuth
import org.veriblock.miners.pop.api.controller.ApiController
import org.veriblock.miners.pop.api.controller.statusPages
import org.veriblock.miners.pop.service.Metrics
import kotlin.reflect.KType

const val API_VERSION = "0.3"

private val logger = createLogger {}

class ApiServer(
    configuration: Configuration,
    private val controllers: List<ApiController>
) {
    private val authUsername: String? = configuration.getString("miner.api.auth.username")
    private val authPassword: String? = configuration.getString("miner.api.auth.password")

    private var server: ApplicationEngine? = null

    fun start(host: String, port: Int) {
        if (server != null) {
            return
        }

        logger.info { "Starting HTTP Service on $host:$port" }

        server = embeddedServer(Netty, host = host, port = port) {
            install(DefaultHeaders)
            install(CallLogging)
            install(CORS) {
                method(HttpMethod.Put)
                anyHost()
                allowNonSimpleContentTypes = true
            }

            val basicAuthProvider = BasicProvider(
                authName = "basicAuth",
                optional = (authUsername == null || authPassword == null)
            )
            installAuth(
                basicAuthProvider = basicAuthProvider,
                authUsername = authUsername,
                authPassword = authPassword
            )

            install(OpenAPIGen) {
                info {
                    version = API_VERSION
                    title = "Altchain PoP Miner API"
                    description = "This is the APM's integrated API, through which you can monitor and control the application"
                    contact {
                        name = "VeriBlock"
                        email = "https://veriblock.org"
                    }
                }
                addModules(basicAuthProvider)
                replaceModule(DefaultSchemaNamer, object : SchemaNamer {
                    val regex = Regex("[A-Za-z0-9_.]+")
                    override fun get(type: KType): String {
                        return type.toString().replace(regex) { it.value.split(".").last() }.replace(Regex(">|<|, "), "_")
                    }
                })
            }

            statusPages()

            install(ContentNegotiation) {
                jackson {
                    enable(
                        DeserializationFeature.WRAP_EXCEPTIONS,
                        DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,
                        DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
                    )
                    enable(SerializationFeature.WRAP_EXCEPTIONS, SerializationFeature.INDENT_OUTPUT)
                    setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
                        indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                        indentObjectsWith(DefaultIndenter("  ", "\n"))
                    })
                    registerModule(JavaTimeModule())
                }
            }

            install(MicrometerMetrics) {
                registry = Metrics.registry
                meterBinders = Metrics.meterBinders
            }

            install(Locations)
            routing {
                authenticate(
                    basicAuthProvider.authName,
                    optional = (authUsername == null || authPassword == null)
                ) {
                    get("openapi.json") {
                        val application = application
                        call.respond(application.openAPIGen.api)
                    }
                    get("api") {
                        call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
                    }
                    get("metrics") {
                        call.respond(Metrics.registry.scrape())
                    }
                }
            }
            apiRouting {
                basicAuth(basicAuthProvider) {
                    route("api") {
                        for (controller in controllers) {
                            with(controller) {
                                registerApi()
                            }
                        }
                    }
                }
            }
            install(SinglePageApplication) {
                folderPath = "/spa-client"
                defaultPage = "index.html"
                ignoreIfContains = "^/api/".toRegex()
            }
        }.start()
    }

    fun shutdown() {
        server?.stop(100, 100)
        server = null
    }
}
