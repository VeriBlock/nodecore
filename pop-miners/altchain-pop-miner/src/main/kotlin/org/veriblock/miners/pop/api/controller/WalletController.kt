package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.put
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import kotlinx.coroutines.runBlocking
import org.veriblock.core.utilities.Utility
import org.veriblock.miners.pop.api.dto.WithdrawRequest
import org.veriblock.miners.pop.api.dto.WithdrawResponse
import org.veriblock.miners.pop.service.AltchainPopMinerService
import org.veriblock.sdk.models.asCoin
import org.veriblock.spv.model.Output
import org.veriblock.spv.model.asStandardAddress

class WalletController(
    private val miner: AltchainPopMinerService,
) : ApiController {

    @Path("withdraw")
    class WithdrawPath

    override fun NormalOpenAPIRoute.registerApi() = route("wallet") {
        put<WithdrawPath, WithdrawResponse, WithdrawRequest>(
            info("Withdraw VBKs to Address")
        ) { _, request ->
            val atomicAmount = Utility.convertDecimalCoinToAtomicLong(request.amount)
            val result = runBlocking {
                miner.spvContext.spvService.sendCoins(
                    null,
                    listOf(Output(request.destinationAddress.asStandardAddress(), atomicAmount.asCoin()))
                )
            }
            val ids = result.map {
                it.toString()
            }
            respond(WithdrawResponse(ids))
        }
    }
}
