package database

import io.ktor.server.application.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun Application.configureDatabase(): R2dbcDatabase {
    val config = environment.config
    val url = config.property("ktor.database.url").getString()
    val user = config.property("ktor.database.user").getString()
    val password = config.property("ktor.database.password").getString()

    fun enc(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)
    val urlWithCredentials = url.replaceFirst("://", "://${enc(user)}:${enc(password)}@")

    val database = R2dbcDatabase.connect(url = urlWithCredentials)
    log.info("Database configured: $url")
    return database
}
