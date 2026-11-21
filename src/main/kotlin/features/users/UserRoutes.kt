package features.users

import io.ktor.http.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import plugins.RateLimits

fun Route.userRoutes(userService: UserService) {
    rateLimit(RateLimits.PSEUDO_CHECK) {
        get("/pseudo-availability") {
            val result = userService.isPseudoAvailable(call.request.queryParameters["pseudo"])
            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.respond(result)
        }
    }


    route("/users") {
        rateLimit(RateLimits.REGISTER) {
            post {
                val request = call.receive<RegisterRequest>()
                val user = userService.register(request)

                call.response.header(HttpHeaders.CacheControl, "no-store")
                call.response.header(HttpHeaders.Location, "/api/v1/users/${user.id}")
                call.respond(HttpStatusCode.Created, user)
            }
        }
    }
}