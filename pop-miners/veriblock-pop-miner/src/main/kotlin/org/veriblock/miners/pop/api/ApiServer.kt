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
import mu.KotlinLogging
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.api.controller.ApiController
import org.veriblock.miners.pop.api.controller.statusPages

private val logger = KotlinLogging.logger {}

const val API_VERSION = "0.3"

class ApiServer(
    vpmConfig: VpmConfig,
    private val controllers: List<ApiController>
) {
    private val config = vpmConfig.api

    private var running = false

    var server: ApplicationEngine? = null

    fun start() {
        if (running) {
            return
        }

        logger.info { "Starting HTTP API on ${config.host}:${config.port}" }

        server = try {
            embeddedServer(Netty, host = config.host, port = config.port) {
                install(DefaultHeaders)
                install(CallLogging)

                statusPages()

                install(ContentNegotiation) {
                    gson()
                }

                install(OpenAPIGen) {
                    info {
                        version = API_VERSION
                        title = "VeriBlock PoP Miner API"
                        description = "This is the VPM's integrated API, through which you can monitor and control the application"
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
        } catch (e: Exception) {
            logger.warn(e) { "Could not start the API" }
            return
        }

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
