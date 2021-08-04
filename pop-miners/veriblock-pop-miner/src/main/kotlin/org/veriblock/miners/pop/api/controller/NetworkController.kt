// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.auth.UserIdPrincipal
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.api.model.NetworkInfoResponse

class NetworkController(
    private val minerConfig: VpmConfig
) : ApiController {

    override fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi() = route("network") {
        get<Unit, NetworkInfoResponse, UserIdPrincipal>(
            info("Returns the Blockchain Network this VPM is configured with")
        ) {
            val networkType = minerConfig.bitcoin.network
            respond(NetworkInfoResponse(networkType.toString()))
        }
    }
}
