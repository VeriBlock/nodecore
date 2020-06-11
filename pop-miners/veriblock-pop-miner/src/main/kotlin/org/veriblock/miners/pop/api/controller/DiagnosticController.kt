package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import org.veriblock.miners.pop.service.DiagnosticService
import org.veriblock.sdk.models.DiagnosticInformation

class DiagnosticController(
    private val diagnosticService: DiagnosticService
) : ApiController {

    override fun NormalOpenAPIRoute.registerApi() = route("debug") {
        get<Unit, DiagnosticInformation>(
            info("Get debug information")
        ) {
           val information = diagnosticService.collectDiagnosticInformation()
            respond(information)
        }
    }
}
