// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.auth.post
import io.ktor.auth.*
import org.veriblock.miners.pop.api.model.WithdrawRequest
import org.veriblock.miners.pop.model.result.Result
import org.veriblock.miners.pop.service.MinerService

class WalletController(
    private val minerService: MinerService
) : ApiController {

    @Path("wallet/btc/withdraw")
    class WithdrawBtcPath

    override fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi() {
        post<WithdrawBtcPath, Result, WithdrawRequest, UserIdPrincipal>(
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
