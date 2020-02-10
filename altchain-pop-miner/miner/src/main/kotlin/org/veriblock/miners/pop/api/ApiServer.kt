// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api

import de.nielsfalk.ktor.swagger.SwaggerSupport
import de.nielsfalk.ktor.swagger.version.shared.Contact
import de.nielsfalk.ktor.swagger.version.shared.Information
import de.nielsfalk.ktor.swagger.version.v2.Swagger
import de.nielsfalk.ktor.swagger.version.v3.OpenApi
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.locations.Locations
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import org.veriblock.core.utilities.Configuration
import org.veriblock.miners.pop.api.controller.ApiController
import org.veriblock.miners.pop.api.controller.statusPages
import java.util.concurrent.TimeUnit

const val API_VERSION = "0.1"

private val logger = KotlinLogging.logger {}

class ApiServer(
    private val controllers: List<ApiController>
) {
    private var running = false

    private val port: Int = Configuration.getInt("miner.api.port") ?: 8080

    var server: ApplicationEngine? = null

    fun start() {
        if (running) {
            return
        }

        logger.info { "Starting HTTP API on port $port" }

        server = embeddedServer(Netty, port = port) {
            install(DefaultHeaders)
            install(CallLogging)

            statusPages()

            install(ContentNegotiation) {
                gson()
            }

            // Documentation
            install(SwaggerSupport) {
                forwardRoot = true
                provideUi = true
                val information = Information(
                    version = API_VERSION,
                    title = "Altchain PoP Miner API",
                    description = "This is the APM's integrated API, through which you can monitor and control the application",
                    contact = Contact("VeriBlock", "https://veriblock.org")
                )
                swagger = Swagger().apply {
                    info = information
                }
                openApi = OpenApi().apply {
                    info = information
                }
            }

            install(Locations)
            routing {
                route("/api") {
                    for (controller in controllers) {
                        with(controller) {
                            registerApi()
                        }
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

        server?.stop(3, 10, TimeUnit.SECONDS)
        running = false
    }
}
