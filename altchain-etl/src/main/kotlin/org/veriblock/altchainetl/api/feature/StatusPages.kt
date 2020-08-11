package org.veriblock.altchainetl.api.feature

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import org.veriblock.altchainetl.logger
import org.veriblock.altchainetl.service.ApplicationException

fun Application.statusPages() {
    install(StatusPages) {
        exception<BadRequestException> {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(it.localizedMessage)
            )
        }
        exception<NotFoundException> {
            call.respond(
                HttpStatusCode.NotFound,
                ApiError(it.localizedMessage)
            )
        }
        exception<ApplicationException> {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(it.message)
            )
        }
        exception<Exception> {
            logger.warn(it) { "Unhandled exception" }

            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError("Internal error! Please, check the server logs")
            )
        }
    }
}

data class ApiError(val message: String)
