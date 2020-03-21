// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.controller

import de.nielsfalk.ktor.swagger.description
import de.nielsfalk.ktor.swagger.get
import de.nielsfalk.ktor.swagger.put
import de.nielsfalk.ktor.swagger.version.shared.Group
import io.ktor.application.call
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Route
import nodecore.miners.pop.api.model.SetConfigRequest
import org.veriblock.core.utilities.Configuration

@Group("Configuration") @Location("/api/config") class config

class ConfigurationController(
    private val configuration: Configuration
) : ApiController {

    override fun Route.registerApi() {
        get<config>(
            "config"
                .description("Get all configuration values")
        ) {
            val configValues = configuration.list()
            call.respond(configValues)
        }
        put<config, SetConfigRequest>(
            "config"
                .description("Set one configuration value")
        ) { _, request ->
            if (request.key == null) {
                throw BadRequestException("'key' must be set")
            }
            if (request.value == null) {
                throw BadRequestException("'value' must be set")
            }
            configuration.setProperty(request.key, request.value)
        }
    }
}
