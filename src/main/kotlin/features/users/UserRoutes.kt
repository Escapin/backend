package features.users

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.userRoutes(userService: UserService) {
    route("/users") {
        post {
            val request = call.receive<RegisterRequest>()
            val user = userService.register(request)

            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.response.header(HttpHeaders.Location, "/api/v1/users/${user.id}")
            call.respond(HttpStatusCode.Created, user)
        }
    }
}