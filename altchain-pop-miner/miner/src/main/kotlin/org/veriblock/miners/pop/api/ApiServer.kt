// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api

import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.locations.Locations
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import org.veriblock.core.utilities.Configuration
import org.veriblock.miners.pop.api.controller.ApiController
import org.veriblock.miners.pop.api.controller.statusPages

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

            install(Locations)
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

        running = true
    }

    fun shutdown() {
        if (!running) {
            return
        }

        server?.stop(3000, 10000)
        running = false
    }
}
