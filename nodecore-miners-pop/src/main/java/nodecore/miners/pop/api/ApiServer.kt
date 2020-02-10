// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api

import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import nodecore.miners.pop.api.controller.ApiController
import nodecore.miners.pop.api.controller.statusPages
import java.util.concurrent.TimeUnit

class ApiServer(
    private val controllers: List<ApiController>
) {

    private var running = false

    var port: Int = 8080
        set(value) {
            if (running) {
                error("Port cannot be set after the server has started")
            }

            field = value
        }

    var address: String = "0.0.0.0"
        set(value) {
            if (running) {
                error("Address cannot be set after the server has started")
            }

            field = value
        }

    var server: ApplicationEngine? = null

    fun start() {
        if (running) {
            return
        }

        logger.info("Starting HTTP API on {}:{}", address, port)

        server = try {
            embeddedServer(Netty, port = port, host = address) {
                install(DefaultHeaders)
                install(CallLogging)

                statusPages()

                install(ContentNegotiation) {
                    gson()
                }

                install(Routing) {
                    route("/api") {
                        for (controller in controllers) {
                            with(controller) {
                                registerApi()
                            }
                        }
                    }
                }
            }.start()
        } catch (e: Exception) {
            logger.warn { "Could not start the API: ${e.message}" }
            return
        }

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

private val logger = KotlinLogging.logger {}
