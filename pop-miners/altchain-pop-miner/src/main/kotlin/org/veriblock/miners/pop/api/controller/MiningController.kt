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
import org.veriblock.miners.pop.api.dto.MineRequest
import org.veriblock.miners.pop.api.dto.MinerInfoResponse
import org.veriblock.miners.pop.api.dto.OperationDetailResponse
import org.veriblock.miners.pop.api.dto.OperationSummaryListResponse
import org.veriblock.miners.pop.api.dto.OperationSummaryResponse
import org.veriblock.miners.pop.api.dto.toDetailedResponse
import org.veriblock.miners.pop.api.dto.toSummaryResponse
import org.veriblock.miners.pop.core.OperationState
import org.veriblock.miners.pop.service.MinerService

class MiningController(
    private val miner: MinerService
) : ApiController {

    @Path("mine")
    class MineActionPath

    @Path("operations")
    class MinerOperationsPath(
        @QueryParam("Status filter (optional)") val status: String?,
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

    @Path("operations/{id}/logs")
    class MinerOperationLogsPath(
        @PathParam("Operation ID") val id: String,
        @QueryParam("Log level (optional, INFO by default)") val level: String?
    )

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
            val result = miner.mine(mineRequest.chainSymbol, mineRequest.height)
            val firstMsg = result.getMessages().first()
            if (result.isFailed) {
                throw CallFailureException("${firstMsg.message} | ${firstMsg.details}")
            }
            respond(
                OperationSummaryResponse(
                    firstMsg.message,
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
            // Get the given status filter
            val filteredOperations = if (location.status != null) {
                val operationState = try {
                    OperationState.valueOf(location.status)
                } catch (e: Exception) {
                    throw BadRequestException("Invalid operation status: ${location.status}")
                }
                allOperations.filter {
                    it.state === operationState
                }
            } else {
                allOperations
            }
            // Get the given offset filter
            val offset = location.offset ?: 0
            // Get the given limit filter
            val limit = location.limit ?: filteredOperations.size
            // Paginate and map operations
            val result = filteredOperations.asSequence().drop(offset).take(limit).map {
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
    }
}
