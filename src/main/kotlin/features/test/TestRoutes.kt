package features.test

import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class PingResponse(val status: String)

@Serializable
data class DbCheckResponse(val status: String, val userCount: Long)

fun Route.testRoutes(service: TestService) {
    route("/test") {
        // Sans base de données : vérifie que le serveur et la sérialisation JSON tournent
        get {
            call.respond(PingResponse("ok"))
        }

        // Avec base de données : vérifie la connexion et que init.sql a bien été appliqué
        get("/db") {
            call.respond(DbCheckResponse("ok", service.countUsers()))
        }
    }
}
