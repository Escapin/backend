package plugins

import features.auth.AuthService
import features.auth.authRoutes
import features.users.UserRepository
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

fun Application.configureRouting(database: R2dbcDatabase) {
    val userRepository = UserRepository(database)
    val authService = AuthService(userRepository)

    routing {
        get("/") {
            call.respondText("Escapin API")
        }
        authRoutes(authService)
    }
}
