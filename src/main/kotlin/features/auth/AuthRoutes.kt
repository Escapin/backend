package features.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import plugins.RateLimits

fun Route.authRoutes(authService: AuthService, googleAuthService: GoogleAuthService, appRedirectUri: String) {
    route("/auth") {
        rateLimit(RateLimits.OAUTH) {
            googleAuthRoute(googleAuthService)

            authenticate("github") {
                get("/github") { }

                get("/github/callback") {
                    val principal = call.principal<OAuthAccessTokenResponse.OAuth2>()
                    if (principal == null) {
                        call.respondRedirect("$appRedirectUri?error=oauth_failed")
                        return@get
                    }

                    try {
                        val token = authService.loginWithGithub(principal.accessToken)
                        call.response.header(HttpHeaders.CacheControl, "no-store")
                        call.respondRedirect("$appRedirectUri?token=$token")

                    } catch (e: Exception) {
                        call.application.log.error("GitHub login failed", e)
                        call.respondRedirect("$appRedirectUri?error=login_failed")
                    }
                }
            }
        }
    }
}

/** `POST /auth/google`, séparée pour être testable sans le provider OAuth GitHub. */
fun Route.googleAuthRoute(googleAuthService: GoogleAuthService) {
    post("/google") {
        val request = call.receive<GoogleAuthRequest>()
        val response = googleAuthService.login(request)

        call.response.header(HttpHeaders.CacheControl, "no-store")
        call.respond(response)
    }
}
