import database.configureDatabase
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import plugins.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureMonitoring()
    val database = configureDatabase()
    val httpClient = configureHttpClient()

    configureSerialization()
    configureStatusPages()
    configureSecurity(httpClient)
    configureRateLimiting()
    configureHttp()
    configureRouting(database, httpClient)
}
