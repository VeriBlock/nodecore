// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api

import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.openAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.veriblock.core.utilities.Configuration
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.api.controller.ApiController
import org.veriblock.miners.pop.api.controller.statusPages

const val API_VERSION = "0.1"

private val logger = createLogger {}

class ApiServer(
    configuration: Configuration,
    private val controllers: List<ApiController>
) {
    private var running = false

    private val port: Int = configuration.getInt("miner.api.port") ?: 8080
    private val host: String = configuration.getString("miner.api.host") ?: "0.0.0.0"

    var server: ApplicationEngine? = null

    fun start() {
        if (running) {
            return
        }

        logger.info { "Starting HTTP API on port $host:$port" }

        server = embeddedServer(Netty, host = host, port = port) {
            install(DefaultHeaders)
            install(CallLogging)

            statusPages()

            install(ContentNegotiation) {
                gson()
            }

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
                schemaNamer = {
                    //rename DTOs from java type name to generator compatible form
                    val regex = Regex("[A-Za-z0-9_.]+")
                    it.toString().replace(regex) { it.value.split(".").last() }.replace(Regex(">|<|, "), "_")
                }
            }

            install(Locations)
            routing {
                get("/openapi.json") {
                    val application = application
                    call.respond(application.openAPIGen.api)
                }
                get("/") {
                    call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
                }
            }
            apiRouting {
                for (controller in controllers) {
                    with(controller) {
                        registerApi()
                    }
                }
            }
        }.start()

        running = true
    }

    fun shutdown() {
        if (!running) {
            return
        }

        server?.stop(100, 100)
        running = false
    }
}
