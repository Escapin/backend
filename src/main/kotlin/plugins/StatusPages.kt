package plugins

import common.ApiException
import common.ProblemDetail
import common.FieldError
import common.respondProblem
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ApiException> { call, e ->
            call.respondProblem(
                ProblemDetail(
                    title = e.status.description,
                    status = e.status.value,
                    detail = e.message,
                    code = e.code,
                    errors = e.errors,
                )
            )
        }

        exception<BadRequestException> { call, e ->
            val missing = generateSequence<Throwable>(e) { it.cause }
                .filterIsInstance<MissingFieldException>()
                .firstOrNull()
                ?.missingFields
                .orEmpty()

            call.respondProblem(
                ProblemDetail(
                    title = HttpStatusCode.BadRequest.description,
                    status = HttpStatusCode.BadRequest.value,
                    detail = if (missing.isEmpty()) "The request body is malformed or incomplete."
                    else "Required fields are missing.",
                    code = "malformed_request",
                    errors = missing
                        .map { FieldError(it, "required", "This field is required.") }
                        .ifEmpty { null },
                )
            )
        }

        exception<ContentTransformationException> { call, _ ->
            call.respondProblem(
                ProblemDetail(
                    title = HttpStatusCode.UnsupportedMediaType.description,
                    status = HttpStatusCode.UnsupportedMediaType.value,
                    detail = "Send a JSON body with the header Content-Type: application/json.",
                    code = "unsupported_media_type",
                )
            )
        }

        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error on ${call.request.local.uri}", cause)
            call.respondProblem(
                ProblemDetail(
                    title = HttpStatusCode.InternalServerError.description,
                    status = HttpStatusCode.InternalServerError.value,
                    code = "internal_error",
                )
            )
        }

        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respondProblem(
                ProblemDetail(
                    title = status.description,
                    status = status.value,
                    detail = "Too many requests. Please try again later.",
                    code = "rate_limited",
                )
            )
        }
    }
}