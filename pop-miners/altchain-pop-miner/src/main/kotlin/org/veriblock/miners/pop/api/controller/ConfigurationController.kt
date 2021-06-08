// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.put
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.auth.*
import org.veriblock.core.utilities.Configuration
import org.veriblock.miners.pop.api.dto.AutoMineConfigDto
import org.veriblock.miners.pop.api.dto.AutoMineRound
import org.veriblock.miners.pop.api.dto.SetConfigRequest
import org.veriblock.miners.pop.api.dto.VbkFeeConfigDto
import org.veriblock.miners.pop.MinerConfig
import org.veriblock.sdk.alt.plugin.PluginService

class ConfigurationController(
    val configuration: Configuration,
    val minerConfig: MinerConfig,
    val pluginService: PluginService
) : ApiController {

    @Path("config")
    class ConfigPath

    @Path("config/automine/{chain}")
    class AutominePath(
        @PathParam("The chain key") val chain: String
    )

    @Path("config/vbk-fee")
    class VbkFeePath

    override fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi() {
        get<ConfigPath, Map<String, String>, UserIdPrincipal>(
            info("Get all configuration values")
        ) {
            val configValues = configuration.list()
            respond(configValues)
        }
        put<ConfigPath, Unit, SetConfigRequest, UserIdPrincipal>(
            info("Sets a new value for a config property (needs restart)")
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
            configuration.saveOverriddenProperties()
            respond(Unit)
        }
        get<AutominePath, AutoMineConfigDto, UserIdPrincipal>(
            info("Get the automine config")
        ) { location ->
            val chain = pluginService[location.chain]
                ?: throw NotFoundException("There is no SI chain with the key ${location.chain}")
            val configValues = AutoMineConfigDto(
                chain.config.availableRoundIndices.map {
                    AutoMineRound(it, chain.config.autoMineRounds.contains(it))
                }
            )
            respond(configValues)
        }
        put<AutominePath, Unit, AutoMineConfigDto, UserIdPrincipal>(
            info("Set the automine config")
        ) { location, request ->
            val chain = pluginService[location.chain]
                ?: throw NotFoundException("There is no SI chain with the key ${location.chain}")
            request.automineRounds.forEach { autoMineRound ->
                if (!chain.config.availableRoundIndices.contains(autoMineRound.round)) {
                    throw BadRequestException("Round ${autoMineRound.round} is not defined in the chain's block round indices")
                }
                if (autoMineRound.enabled) {
                    chain.config.autoMineRounds.add(autoMineRound.round)
                } else {
                    chain.config.autoMineRounds.remove(autoMineRound.round)
                }
            }
            respond(Unit)
        }
        get<VbkFeePath, VbkFeeConfigDto, UserIdPrincipal>(
            info("Gets the current VBK fee config")
        ) {
            val configValues = VbkFeeConfigDto(
                minerConfig.maxFee,
                minerConfig.feePerByte
            )
            respond(configValues)
        }
        put<VbkFeePath, Unit, VbkFeeConfigDto, UserIdPrincipal>(
            info("Sets the current VBK fee config")
        ) { _, request ->
            if (request.maxFee != null) {
                minerConfig.maxFee = request.maxFee
            }
            if (request.feePerByte != null) {
                minerConfig.feePerByte = request.feePerByte
            }
            respond(Unit)
        }
    }
}
