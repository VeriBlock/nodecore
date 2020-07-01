// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import ch.qos.logback.classic.Level
import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import org.veriblock.miners.pop.api.dto.ConfiguredAltchain
import org.veriblock.miners.pop.api.dto.ConfiguredAltchainList
import org.veriblock.miners.pop.api.dto.MineRequest
import org.veriblock.miners.pop.api.dto.MinerInfoResponse
import org.veriblock.miners.pop.api.dto.OperationDetailResponse
import org.veriblock.miners.pop.api.dto.OperationSummaryListResponse
import org.veriblock.miners.pop.api.dto.OperationSummaryResponse
import org.veriblock.miners.pop.api.dto.OperationWorkflow
import org.veriblock.miners.pop.api.dto.toDetailedResponse
import org.veriblock.miners.pop.api.dto.toSummaryResponse
import org.veriblock.miners.pop.service.ApmOperationExplainer
import org.veriblock.miners.pop.service.MinerService
import org.veriblock.sdk.alt.plugin.PluginService

class MiningController(
    private val miner: MinerService,
    private val operationExplainer: ApmOperationExplainer,
    private val pluginService: PluginService
) : ApiController {

    @Path("mine")
    class MineActionPath

    @Path("operations")
    class MinerOperationsPath(
        @QueryParam("Pagination limit (optional)") val limit: Int?,
        @QueryParam("Pagination offset (optional)") val offset: Int?
    )

    @Path("operations/{id}")
    class MinerOperationPath(
        @PathParam("Operation ID") val id: String
    )

    @Path("operations/{id}/cancel")
    class CancelOperationPath(
        @PathParam("Operation ID") val id: String
    )

    @Path("operations/{id}/workflow")
    class MinerOperationWorkflowPath(
        @PathParam("Operation ID") val id: String
    )

    @Path("operations/{id}/logs")
    class MinerOperationLogsPath(
        @PathParam("Operation ID") val id: String,
        @QueryParam("Log level (optional, INFO by default)") val level: String?
    )

    @Path("configured-altchains")
    class MinerConfiguredAltchainsPath

    override fun NormalOpenAPIRoute.registerApi() = route("miner") {
        get<Unit, MinerInfoResponse>(
            info("Get miner data")
        ) {
            val responseModel = MinerInfoResponse(
                vbkAddress = miner.getAddress(),
                vbkBalance = miner.getBalance()?.confirmedBalance?.atomicUnits ?: 0
            )
            respond(responseModel)
        }
        post<MineActionPath, OperationSummaryResponse, MineRequest>(
            info("Start mining operation")
        ) { _, mineRequest ->
            val operationId = miner.mine(mineRequest.chainSymbol, mineRequest.height)
            respond(
                OperationSummaryResponse(
                    operationId,
                    mineRequest.chainSymbol,
                    mineRequest.height,
                    "Starting...",
                    ""
                )
            )
        }
        get<MinerOperationsPath, OperationSummaryListResponse>(
            info("Get operations list")
        ) { location ->
            // Get all the operations
            val allOperations = miner.getOperations()
            // Get the given offset filter
            val offset = location.offset ?: 0
            // Get the given limit filter
            val limit = location.limit ?: allOperations.size
            // Paginate and map operations
            val result = allOperations.asSequence().drop(offset).take(limit).map {
                it.toSummaryResponse()
            }.toList()
            respond(OperationSummaryListResponse(result))
        }
        get<MinerOperationPath, OperationDetailResponse>(
            info("Get operation details")
        ) { location ->
            val id = location.id

            val operationState = miner.getOperation(id)
                ?: throw NotFoundException("Operation $id not found")

            val responseModel = operationState.toDetailedResponse()
            respond(responseModel)
        }
        post<CancelOperationPath, Unit, Unit>(
            info("Cancel an operation")
        ) { location, _ ->
            val result = miner.cancelOperation(location.id)
            respond(result)
        }
        get<MinerOperationLogsPath, List<String>>(
            info("Get the operation logs")
        ) { location ->
            val level: Level = Level.toLevel(location.level, Level.INFO)
            val operation = miner.getOperation(location.id)
                ?: throw NotFoundException("Operation ${location.id} not found")

            val responseModel = operation.getLogs(level).map { it.toString() }
            respond(responseModel)
        }
        get<MinerOperationWorkflowPath, OperationWorkflow>(
            info("Get operation workflow")
        ) { location ->
            val id = location.id

            val operation = miner.getOperation(id)
                ?: throw NotFoundException("Operation $id not found")

            val workflow = operationExplainer.explainOperation(operation)
            respond(workflow)
        }
        get<MinerConfiguredAltchainsPath, ConfiguredAltchainList>(
            info("Get configured altchains")
        ) {
            val altchains = pluginService.getPlugins().values.map {
                ConfiguredAltchain(
                    it.id,
                    it.key,
                    it.name,
                    it.getPayoutInterval()
                )
            }
            respond(ConfiguredAltchainList(altchains))
        }
    }
}
