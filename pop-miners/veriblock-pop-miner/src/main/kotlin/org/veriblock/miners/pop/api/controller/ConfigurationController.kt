// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.Request
import com.papsign.ktor.openapigen.annotations.Response
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.put
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.auth.UserIdPrincipal
import org.veriblock.core.utilities.Configuration
import org.veriblock.miners.pop.AutoMineConfig
import org.veriblock.miners.pop.api.model.SetConfigRequest
import org.veriblock.miners.pop.automine.AutoMineEngine
import org.veriblock.miners.pop.service.BitcoinService

class ConfigurationController(
    private val configuration: Configuration,
    private val autoMineEngine: AutoMineEngine,
    private val bitcoinService: BitcoinService
) : ApiController {

    @Path("automine")
    class AutominePath

    @Path("btc-fee")
    class BtcFeePath

    override fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi() = route("config") {
        get<Unit, Map<String, String>, UserIdPrincipal>(
            info("Get all configuration values")
        ) {
            val configValues = configuration.list()
            respond(configValues)
        }
        put<Unit, Unit, SetConfigRequest, UserIdPrincipal>(
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
        ) {
            val configValues = AutoMineConfigDto(
                autoMineEngine.config.round1,
                autoMineEngine.config.round2,
                autoMineEngine.config.round3,
                autoMineEngine.config.round4
            )
            respond(configValues)
        }
        put<AutominePath, Unit, AutoMineConfigDto, UserIdPrincipal>(
            info("Set the automine config")
        ) { _, request ->
            autoMineEngine.config = AutoMineConfig(
                request.round1 ?: autoMineEngine.config.round1,
                request.round2 ?: autoMineEngine.config.round2,
                request.round3 ?: autoMineEngine.config.round3,
                request.round4 ?: autoMineEngine.config.round4
            )
            respond(Unit)
        }
        get<BtcFeePath, BtcFeeConfigDto, UserIdPrincipal>(
            info("Get the btcfee config")
        ) {
            val configValues = BtcFeeConfigDto(
                bitcoinService.maxFee,
                bitcoinService.feePerKb
            )
            respond(configValues)
        }
        put<BtcFeePath, Unit, BtcFeeConfigDto, UserIdPrincipal>(
            info("Set the btcfee config")
        ) { _, request ->
            if (request.maxFee != null) {
                bitcoinService.maxFee = request.maxFee
            }
            if (request.feePerKB != null) {
                bitcoinService.feePerKb = request.feePerKB
            }
            respond(Unit)
        }
    }
}

@Request("Auto mine configuration request")
@Response("Auto mine configuration")
data class AutoMineConfigDto(
    val round1: Boolean?,
    val round2: Boolean?,
    val round3: Boolean?,
    val round4: Boolean?
)

@Request("Btc fee configuration request")
@Response("Btc fee configuration")
class BtcFeeConfigDto(
    val maxFee: Long?,
    val feePerKB: Long?
)
