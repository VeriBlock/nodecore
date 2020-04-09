// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import mu.KotlinLogging
import org.veriblock.miners.pop.EventBus
import java.lang.Thread.sleep
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

class QuitController : ApiController {

    @Path("quit")
    data class QuitPath(
        @QueryParam("External quit") val restart: Boolean?
    )

    override fun NormalOpenAPIRoute.registerApi() {
        post<QuitPath, Unit, Unit>(
            info("Exits the application")
        ) { location, _ ->
            logger.info("Terminating the miner now")
            val quitReason = if (location.restart == true) 1 else 0
            val quitExecutor = Executors.newSingleThreadExecutor()
            quitExecutor.submit {
                sleep(100)
                EventBus.programQuitEvent.trigger(quitReason)
            }

            respond(Unit)
        }
    }
}
