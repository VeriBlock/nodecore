// VeriBlock NodeCore
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.api.controller

import com.google.inject.Inject
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import nodecore.miners.pop.api.model.MineRequestPayload
import nodecore.miners.pop.api.model.MinerInfoResponse
import nodecore.miners.pop.api.model.toResponse
import nodecore.miners.pop.contracts.PoPMiner

class MiningController @Inject constructor(
    private val miner: PoPMiner
) : ApiController {

    override fun Route.registerApi() {
        get("/operations") {
            val operationSummaries = miner.listOperations()
                ?: throw NotFoundException("No operations found")

            val responseModel = operationSummaries.map { it.toResponse() }
            call.respond(responseModel)
        }
        get("/operations/{id}") {
            val id = call.parameters["id"]!!

            val operationState = miner.getOperationState(id)
                ?: throw NotFoundException("Operation $id not found")

            val responseModel = operationState.toResponse()
            call.respond(responseModel)
        }
        post("/mine") {
            val payload: MineRequestPayload = call.receive()

            val result = miner.mine(payload.block)

            val responseModel = result.toResponse()
            call.respond(responseModel)
        }
        get("/miner") {
            val responseModel = MinerInfoResponse(
                bitcoinBalance = miner.bitcoinBalance.longValue(),
                bitcoinAddress = miner.bitcoinReceiveAddress,
                minerAddress = miner.minerAddress,
                walletSeed = miner.walletSeed
            )
            call.respond(responseModel)
        }
    }
}
