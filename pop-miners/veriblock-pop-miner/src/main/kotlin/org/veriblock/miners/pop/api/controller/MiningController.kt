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

    override fun NormalOpenAPIRoute.registerApi() {
        post<MineActionPath, MineResultResponse, MineRequest>(
            info("Start mining operation")
        ) { _, request ->
            val result = minerService.mine(request.block)

            val responseModel = result.toResponse()
            respond(responseModel)
        }
        get<MinerPath, MinerInfoResponse>(
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
        get<MinerOperationsPath, OperationSummaryListResponse>(
            info("Get operations list")
        ) {
            val operationSummaries = minerService.listOperations()

            val responseModel = operationSummaries.map { it.toResponse() }
            respond(OperationSummaryListResponse(responseModel))
        }
        get<MinerOperationPath, OperationDetailResponse>(
            info("Get operation details")
        ) { location ->
            val id = location.id

            val operationState = minerService.getOperation(id)
                ?: throw NotFoundException("Operation $id not found")

            val responseModel = operationState.toResponse()
            respond(responseModel)
        }
        post<CancelOperationPath, Unit, Unit>(
            info("Cancel an operation")
        ) { location, _ ->
            val result = minerService.cancelOperation(location.id)
            respond(result)
        }
        get<MinerOperationLogsPath, List<String>>(
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
