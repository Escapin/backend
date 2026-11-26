package plugins

import features.auth.AuthService
import features.auth.GithubClient
import features.auth.GoogleAuthService
import features.auth.JwksGoogleTokenVerifier
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
    val tokenService = TokenService.from(environment.config)

    val authService = AuthService(
        userRepository = userRepository,
        githubClient = GithubClient(httpClient),
        tokenService = tokenService,
    )
    val googleAuthService = GoogleAuthService(
        userRepository = userRepository,
        tokenVerifier = JwksGoogleTokenVerifier(environment.config.property("OAuth2.google.web-client-id").getString()),
        tokenService = tokenService,
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
            authRoutes(authService, googleAuthService, appRedirectUri)
            userRoutes(userService)
        }
    }
}