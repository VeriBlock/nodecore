// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.auth.UserIdPrincipal
import org.apache.logging.log4j.Level
import org.veriblock.core.MineException
import org.veriblock.miners.pop.api.model.MineRequest
import org.veriblock.miners.pop.api.model.MineResultResponse
import org.veriblock.miners.pop.api.model.MinerInfoResponse
import org.veriblock.miners.pop.api.model.OperationDetailResponse
import org.veriblock.miners.pop.api.model.OperationSummaryListResponse
import org.veriblock.miners.pop.api.model.toResponse
import org.veriblock.miners.pop.service.MinerService

class MiningController(
    private val minerService: MinerService
) : ApiController {

    @Path("mine")
    class MineActionPath

    @Path("miner")
    class MinerPath

    @Path("operations")
    class MinerOperationsPath

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

    override fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi() {
        post<MineActionPath, MineResultResponse, MineRequest, UserIdPrincipal>(
            info("Start mining operation")
        ) { _, request ->
            val result = try {
                minerService.mine(request.block)
            } catch(exception: MineException) {
                throw BadRequestException("Failed to start operation: ${exception.message}")
            }
            val responseModel = result.toResponse()
            respond(responseModel)
        }
        get<MinerPath, MinerInfoResponse, UserIdPrincipal>(
            info("Get miner data")
        ) {
            val responseModel = MinerInfoResponse(
                bitcoinBalance = minerService.getBitcoinBalance().longValue(),
                bitcoinAddress = minerService.getBitcoinReceiveAddress(),
                minerAddress = minerService.getMinerAddress(),
                walletSeed = minerService.getWalletSeed()
            )
            respond(responseModel)
        }
        get<MinerOperationsPath, OperationSummaryListResponse, UserIdPrincipal>(
            info("Get operations list")
        ) {
            val operationSummaries = minerService.listOperations()

            val responseModel = operationSummaries.map { it.toResponse() }
            respond(OperationSummaryListResponse(responseModel))
        }
        get<MinerOperationPath, OperationDetailResponse, UserIdPrincipal>(
            info("Get operation details")
        ) { location ->
            val id = location.id

            val operationState = minerService.getOperation(id)
                ?: throw NotFoundException("Operation $id not found")

            val responseModel = operationState.toResponse()
            respond(responseModel)
        }
        post<CancelOperationPath, Unit, Unit, UserIdPrincipal>(
            info("Cancel an operation")
        ) { location, _ ->
            val result = minerService.cancelOperation(location.id)
            respond(result)
        }
        get<MinerOperationLogsPath, List<String>, UserIdPrincipal>(
            info("Get the operation logs")
        ) { location ->
            val level: Level = Level.toLevel(location.level, Level.INFO)
            val operation = minerService.getOperation(location.id)
                ?: throw NotFoundException("Operation ${location.id} not found")

            val responseModel = operation.getLogs(level).map { it.toString() }
            respond(responseModel)
        }
    }
}
