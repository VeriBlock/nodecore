// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import de.nielsfalk.ktor.swagger.description
import de.nielsfalk.ktor.swagger.get
import de.nielsfalk.ktor.swagger.notFound
import de.nielsfalk.ktor.swagger.ok
import de.nielsfalk.ktor.swagger.post
import de.nielsfalk.ktor.swagger.responds
import io.ktor.application.call
import io.ktor.locations.Location
import io.ktor.response.respond
import io.ktor.routing.Route
import org.veriblock.miners.pop.Miner
import org.veriblock.miners.pop.api.dto.MineRequest
import org.veriblock.miners.pop.api.dto.MinerInfoResponse
import org.veriblock.miners.pop.api.dto.OperationDetailResponse
import org.veriblock.miners.pop.api.dto.OperationSummaryListResponse
import org.veriblock.miners.pop.api.dto.OperationSummaryResponse
import org.veriblock.miners.pop.api.dto.toDetailedResponse
import org.veriblock.miners.pop.api.dto.toSummaryResponse
import org.veriblock.miners.pop.core.OperationStatus

@Location("/api/miner")
class miner
@Location("/api/miner/mine")
class mineAction
@Location("/api/miner/operations")
class minerOperations(val status: String?, val limit: Int?, val offset: Int?)
@Location("/api/miner/operations/{id}")
class minerOperation(val id: String)

class MiningController(
    private val miner: Miner
) : ApiController {

    override fun Route.registerApi() {
        get<miner>(
            "miner"
                .description("Get miner data")
                .responds(
                    ok<MinerInfoResponse>()
                )
        ) {
            val responseModel = MinerInfoResponse(
                vbkAddress = miner.getAddress(),
                vbkBalance = miner.getBalance()?.confirmedBalance?.atomicUnits ?: 0
            )
            call.respond(responseModel)
        }
        post<mineAction, MineRequest>(
            "mine"
                .description("Start mining operation")
                .responds(
                    ok<OperationSummaryResponse>()
                )
        ) { _, mineRequest ->
            val result = miner.mine(mineRequest.chainSymbol, mineRequest.height)
            val firstMsg = result.getMessages().first()
            if (result.isFailed) {
                throw CallFailureException("${firstMsg.message} | ${firstMsg.details}")
            }
            call.respond(
                OperationSummaryResponse(
                    firstMsg.message,
                    mineRequest.chainSymbol,
                    mineRequest.height,
                    "Starting..."
                )
            )
        }
        get<minerOperations>(
            "operations"
                .description("Get operations list")
                .responds(
                    ok<OperationSummaryListResponse>()
                )
        ) { location ->
            // Get all the operations
            val allOperations = miner.getOperations()
            // Get the given status filter
            val filteredOperations = if (location.status != null) {
                val operationStatus = try {
                    OperationStatus.valueOf(location.status)
                } catch (e: Exception) {
                    throw BadRequestException("Invalid operation status: ${location.status}")
                }
                allOperations.filter {
                    it.status === operationStatus
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
            call.respond(result)
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

            val operationState = miner.getOperation(id)
                ?: throw NotFoundException("Operation $id not found")

            val responseModel = operationState.toDetailedResponse()
            call.respond(responseModel)
        }
    }
}
