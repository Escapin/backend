import database.configureDatabase
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import plugins.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val database = configureDatabase()

    configureSerialization()
    configureStatusPages()
    configureSecurity()
    configureRateLimiting()
    configureHttp()
    configureRouting(database)
}
