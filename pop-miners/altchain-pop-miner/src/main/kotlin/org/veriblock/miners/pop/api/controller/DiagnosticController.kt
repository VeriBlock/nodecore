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
import io.ktor.auth.*
import org.veriblock.miners.pop.service.DiagnosticService
import org.veriblock.sdk.models.DiagnosticInformation

class DiagnosticController(
    private val diagnosticService: DiagnosticService
) : ApiController {

    override fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi() = route("debug") {
        get<Unit, DiagnosticInformation, UserIdPrincipal>(
            info("Get debug information")
        ) {
            val information = diagnosticService.collectDiagnosticInformation()
            respond(information)
        }
    }
}
