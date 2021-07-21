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
import com.papsign.ktor.openapigen.route.route
import io.ktor.auth.UserIdPrincipal
import org.veriblock.core.utilities.AddressUtility
import org.veriblock.core.utilities.Utility
import org.veriblock.miners.pop.NodeCoreLiteKit
import org.veriblock.miners.pop.api.dto.WithdrawRequest
import org.veriblock.miners.pop.api.dto.WithdrawResponse
import org.veriblock.miners.pop.service.AltchainPopMinerService

class WalletController(
    private val miner: AltchainPopMinerService,
    private val nodeCoreLiteKit: NodeCoreLiteKit
) : ApiController {

    @Path("withdraw")
    class WithdrawPath

    override fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi() = route("wallet") {
        post<WithdrawPath, WithdrawResponse, WithdrawRequest, UserIdPrincipal>(
            info("Withdraw VBKs to Address")
        ) { _, request ->
            val atomicAmount = Utility.convertDecimalCoinToAtomicLong(request.amount)
            val destinationAddress = if (AddressUtility.isValidStandardOrMultisigAddress(request.destinationAddress)) {
                request.destinationAddress
            } else {
                throw BadRequestException("${request.destinationAddress} is not a valid standard address")
            }
            throw BadRequestException("withdraw is not implemented yet")
        }
    }
}
