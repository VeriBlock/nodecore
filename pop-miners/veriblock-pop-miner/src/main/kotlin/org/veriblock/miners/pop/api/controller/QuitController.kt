// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import de.nielsfalk.ktor.swagger.description
import de.nielsfalk.ktor.swagger.post
import de.nielsfalk.ktor.swagger.version.shared.Group
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Route
import mu.KotlinLogging
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.api.model.EmptyRequest
import java.lang.Thread.sleep
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

@Group("Quit")
@Location("/api/quit")
class quit(val restart: Boolean = false)

class QuitController : ApiController {

    override fun Route.registerApi() {
        post<quit, EmptyRequest>(
            "quit"
                .description("Exits the application")
        ) { location, _ ->
            logger.info("Terminating the miner now")
            val quitReason = if (location.restart) 1 else 0
            val quitExecutor = Executors.newSingleThreadExecutor()
            quitExecutor.submit {
                sleep(100)
                EventBus.programQuitEvent.trigger(quitReason)
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}
