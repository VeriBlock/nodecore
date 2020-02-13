package nodecore.miners.pop.api.controller

import com.google.gson.JsonSyntaxException
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentTransformationException
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import mu.KotlinLogging
import java.io.IOException

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
            call.respond(HttpStatusCode.BadRequest, it.message)
        }
        exception<JsonSyntaxException> {
            call.respond(HttpStatusCode.BadRequest, ApiError(""))
        }
        exception<NotFoundException> {
            call.respond(HttpStatusCode.NotFound, ApiError(it.message))
        }
        exception<CallFailureException> {
            call.respond(HttpStatusCode.InternalServerError, ApiError(it.message))
        }
        exception<ContentTransformationException> {
            call.respond(HttpStatusCode.InternalServerError, ApiError("Unable to process request! Make sure the request is properly formed."))
        }
        exception<IOException> {
            logger.warn("Unhandled exception", it)
            call.respond(HttpStatusCode.InternalServerError, ApiError("Unhandled exception! Check the console logs for details."))
        }
        exception<Exception> {
            logger.warn("Unhandled exception", it)
            call.respond(HttpStatusCode.InternalServerError, ApiError("Unhandled exception! Check the console logs for details."))
        }
    }
}

