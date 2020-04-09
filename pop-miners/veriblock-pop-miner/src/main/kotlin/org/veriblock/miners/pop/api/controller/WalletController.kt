package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import org.veriblock.miners.pop.api.model.WithdrawRequest
import org.veriblock.miners.pop.model.result.Result
import org.veriblock.miners.pop.service.MinerService

class WalletController(
    private val minerService: MinerService
) : ApiController {

    @Path("wallet/btc/withdraw")
    class WithdrawBtcPath

    override fun NormalOpenAPIRoute.registerApi() {
        post<WithdrawBtcPath, Result, WithdrawRequest>(
            info("Withdraws BTC from the PoP Miner")
        ) { _, request ->
            val address: String = request.destinationAddress
                ?: throw BadRequestException("Parameter 'destinationAddress' was not supplied")
            val amountString: String = request.amount
                ?: throw BadRequestException("Parameter 'amount' was not supplied")
            val amount = amountString.toBigDecimalOrNull()
                ?: throw BadRequestException("Parameter 'amount' is not a valid number")
            val result = minerService.sendBitcoinToAddress(address, amount)
            if (result.didFail()) {
                throw CallFailureException(
                    result.messages.firstOrNull()?.let { rm ->
                        rm.message + (rm.details.firstOrNull()?.let { "; $it" } ?: "")
                    } ?: "Failed to send $amount Bitcoins to $address"
                )
            }
        }
    }
}
