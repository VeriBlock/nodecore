// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.controller

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.put
import nodecore.miners.pop.Configuration
import nodecore.miners.pop.api.model.SetConfigRequestPayload

class ConfigurationController(
    private val configuration: Configuration
) : ApiController {

    override fun Route.registerApi() {
        get("/config") {
            val configValues = configuration.list()
            val map = configValues.associate { configValue ->
                configValue.split("=").let {
                    it[0] to it[1]
                }
            }
            call.respond(map)
        }
        put("/config") {
            val payload: SetConfigRequestPayload = call.receive()
            val result = configuration.setProperty(payload.key, payload.value)
            call.respond(
                if (result.didFail()) HttpStatusCode.InternalServerError else HttpStatusCode.OK,
                result
            )
        }
    }
}
