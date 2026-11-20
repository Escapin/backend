package plugins

import features.test.TestService
import features.test.testRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

fun Application.configureRouting(database: R2dbcDatabase) {
    val testService = TestService(database)

    routing {
        get("/") {
            call.respondText("Escapin API")
        }
        testRoutes(testService)
    }
}
