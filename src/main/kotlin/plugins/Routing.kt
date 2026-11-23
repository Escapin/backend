package plugins

import features.auth.AuthService
import features.auth.GithubClient
import features.auth.authRoutes
import features.users.UserRepository
import features.users.UserService
import features.users.userRoutes
import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import security.CompromisedPasswordChecker
import security.PasswordHasher
import security.TokenService

fun Application.configureRouting(database: R2dbcDatabase, httpClient: HttpClient) {
    val userRepository = UserRepository(database)

    val authService = AuthService(
        userRepository = userRepository,
        githubClient = GithubClient(httpClient),
        tokenService = TokenService.from(environment.config),
    )
    val userService = UserService(
        userRepository = userRepository,
        passwordHasher = PasswordHasher(),
        compromisedPasswordChecker = CompromisedPasswordChecker()
    )
    val appRedirectUri = environment.config.property("OAuth2.github.app-redirect").getString()

    routing {
        get("/") {
            call.respondText("Escapin API")
        }

        route("/api/v1") {
            authRoutes(authService, appRedirectUri)
            userRoutes(userService)
        }
    }
}