// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.controller

import de.nielsfalk.ktor.swagger.description
import de.nielsfalk.ktor.swagger.get
import de.nielsfalk.ktor.swagger.notFound
import de.nielsfalk.ktor.swagger.ok
import de.nielsfalk.ktor.swagger.post
import de.nielsfalk.ktor.swagger.responds
import de.nielsfalk.ktor.swagger.version.shared.Group
import io.ktor.application.call
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Route
import nodecore.miners.pop.MinerService
import nodecore.miners.pop.api.model.MineRequest
import nodecore.miners.pop.api.model.MineResultResponse
import nodecore.miners.pop.api.model.MinerInfoResponse
import nodecore.miners.pop.api.model.OperationDetailResponse
import nodecore.miners.pop.api.model.OperationSummaryListResponse
import nodecore.miners.pop.api.model.toResponse

@Group("Mining")
@Location("/api/operations")
class minerOperations
@Group("Mining")
@Location("/api/operations/{id}")
class minerOperation(val id: String)
@Group("Mining")
@Location("/api/mine")
class mineAction
@Group("Mining")
@Location("/api/miner")
class miner

class MiningController(
    private val minerService: MinerService
) : ApiController {

    override fun Route.registerApi() {
        get<minerOperations>(
            "operations"
                .description("Get operations list")
                .responds(
                    ok<OperationSummaryListResponse>()
                )
        ) {
            val operationSummaries = minerService.listOperations()

            val responseModel = operationSummaries.map { it.toResponse() }
            call.respond(OperationSummaryListResponse(responseModel))
        }
        get<minerOperation>(
            "operation"
                .description("Get operation details")
                .responds(
                    ok<OperationDetailResponse>(),
                    notFound<ApiError>()
                )
        ) { location ->
            val id = location.id

            val operationState = minerService.getOperation(id)
                ?: throw NotFoundException("Operation $id not found")

            val responseModel = operationState.toResponse()
            call.respond(responseModel)
        }
        post<mineAction, MineRequest>(
            "mine"
                .description("Start mining operation")
                .responds(
                    ok<MineResultResponse>()
                )
        ) { location, payload ->
            val result = minerService.mine(payload.block)

            val responseModel = result.toResponse()
            call.respond(responseModel)
        }
        get<miner>(
            "miner"
                .description("Get miner data")
                .responds(
                    ok<MinerInfoResponse>()
                )
        ) {
            val responseModel = MinerInfoResponse(
                bitcoinBalance = minerService.getBitcoinBalance().longValue(),
                bitcoinAddress = minerService.getBitcoinReceiveAddress(),
                minerAddress = minerService.getMinerAddress(),
                walletSeed = minerService.getWalletSeed()
            )
            call.respond(responseModel)
        }
    }
}
