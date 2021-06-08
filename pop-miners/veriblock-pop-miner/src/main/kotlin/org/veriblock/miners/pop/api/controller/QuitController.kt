// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.auth.UserIdPrincipal
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

    override fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi() {
        post<QuitPath, Unit, Unit, UserIdPrincipal>(
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
