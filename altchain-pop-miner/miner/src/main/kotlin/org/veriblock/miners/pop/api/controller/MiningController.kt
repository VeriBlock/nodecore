// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.response.respond
import io.ktor.routing.Route
import org.veriblock.miners.pop.Miner
import org.veriblock.miners.pop.api.model.MinerInfoResponse
import org.veriblock.miners.pop.api.model.toDetailedResponse
import org.veriblock.miners.pop.api.model.toSummaryResponse

@KtorExperimentalLocationsAPI
class MiningController(
    private val miner: Miner
) : ApiController {
    @Location("/miner") class MinerLocation {
        @Location("/operations") class Operations {
            @Location("/{id}") class ById(val id: String)
        }
        @Location("/mine") class Mine(val chainSymbol: String, val height: Int? = null)
    }

    override fun Route.registerApi() {
        get<MinerLocation> {
            val responseModel = MinerInfoResponse(
                vbkAddress = miner.getAddress(),
                vbkBalance = miner.getBalance()?.confirmedBalance?.atomicUnits ?: 0
            )
            call.respond(responseModel)
        }
        post<MinerLocation.Mine> { location ->
            val result = miner.mine(location.chainSymbol, location.height)
            val firstMsg = result.getMessages().first()
            if (result.isFailed) {
                throw CallFailureException("${firstMsg.message} | ${firstMsg.details}")
            }
            call.respond(firstMsg.message)
        }
        get<MinerLocation.Operations> {
            val operationSummaries = miner.getOperations()

            val responseModel = operationSummaries.map { it.toSummaryResponse() }
            call.respond(responseModel)
        }
        get<MinerLocation.Operations.ById> { location ->
            val id = location.id

            val operationState = miner.getOperation(id)
                ?: throw NotFoundException("Operation $id not found")

            val responseModel = operationState.toDetailedResponse()
            call.respond(responseModel)
        }
    }
}
