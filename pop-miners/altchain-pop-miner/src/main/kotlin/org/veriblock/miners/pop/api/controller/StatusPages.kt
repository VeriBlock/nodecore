package org.veriblock.miners.pop.api.controller

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BadRequestException(override val message: String) : Exception()
class NotFoundException(override val message: String) : Exception()
class CallFailureException(override val message: String) : Exception()

data class ApiError(
    val message: String
)

fun Application.statusPages() {
    install(StatusPages) {
        exception<BadRequestException> {
            call.respond(HttpStatusCode.BadRequest, ApiError(it.message))
        }
        exception<JsonParseException> {
            call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
        }
        exception<JsonMappingException> {
            call.respond(HttpStatusCode.BadRequest, it.message ?: "Unable to parse JSON body")
        }
        exception<MissingKotlinParameterException> {
            call.respond(HttpStatusCode.BadRequest, "Missing body parameter: ${it.parameter.name}")
        }
        exception<NotFoundException> {
            call.respond(HttpStatusCode.NotFound, ApiError(it.message))
        }
        exception<CallFailureException> {
            call.respond(HttpStatusCode.InternalServerError, ApiError(it.message))
        }
        exception<Throwable> {
            logger.warn(it) { "Unhandled exception" }
            call.respond(HttpStatusCode.InternalServerError, ApiError("Unhandled exception! Check the console logs for details."))
        }
    }
}

