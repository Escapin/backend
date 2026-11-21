package common

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ProblemDetail(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val detail: String? = null,
    val code: String? = null,
    val errors: List<FieldError>? = null,
)

@Serializable
data class FieldError(val field: String, val code: String, val message: String)

private val problemJson = Json {
    encodeDefaults = true
    explicitNulls = false
}

private val ProblemJsonContentType = ContentType("application", "problem+json")

suspend fun ApplicationCall.respondProblem(problem: ProblemDetail) {
    respondText(
        text = problemJson.encodeToString(ProblemDetail.serializer(), problem),
        contentType = ProblemJsonContentType,
        status = HttpStatusCode.fromValue(problem.status),
    )
}

open class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
    val errors: List<FieldError>? = null,
) : RuntimeException(message)

class ValidationException(errors: List<FieldError>) :
    ApiException(HttpStatusCode.UnprocessableEntity, "validation_failed", "One or more fields are invalid.", errors)

class ConflictException(code: String, message: String) :
    ApiException(HttpStatusCode.Conflict, code, message)