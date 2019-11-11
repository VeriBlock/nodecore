// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.controller

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import mu.KotlinLogging
import nodecore.miners.pop.InternalEventBus
import nodecore.miners.pop.events.ProgramQuitEvent
import java.lang.Thread.sleep
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

class QuitController : ApiController {

    override fun Route.registerApi() {
        post("/quit") {
            logger.info("Terminating the miner now")
            val quitExecutor = Executors.newSingleThreadExecutor()
            quitExecutor.submit {
                sleep(1000)
                InternalEventBus.getInstance().post(ProgramQuitEvent(1))
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}
