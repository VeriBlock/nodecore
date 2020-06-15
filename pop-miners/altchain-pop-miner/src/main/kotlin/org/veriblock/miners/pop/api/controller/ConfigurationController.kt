package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.put
import com.papsign.ktor.openapigen.route.response.respond
import org.veriblock.core.utilities.Configuration
import org.veriblock.miners.pop.api.dto.SetConfigRequest

class ConfigurationController(
    val configuration: Configuration
) : ApiController {

    @Path("config")
    class ConfigPath

    override fun NormalOpenAPIRoute.registerApi() {
        get<ConfigPath, Map<String, String>>(
            info("Get all configuration values")
        ) {
            val configValues = configuration.list()
            respond(configValues)
        }
        put<ConfigPath, Unit, SetConfigRequest>(
            info("Set one configuration value")
        ) { _, request ->
            if (request.key == null) {
                throw BadRequestException("'key' must be set")
            }
            if (request.value == null) {
                throw BadRequestException("'value' must be set")
            }
            val configValues = configuration.list()
            if (!configValues.containsKey(request.key)) {
                throw BadRequestException("'${request.key}' is not part of the configurable properties")
            }
            configuration.setProperty(request.key, request.value)
            respond(Unit)
        }
    }
}
