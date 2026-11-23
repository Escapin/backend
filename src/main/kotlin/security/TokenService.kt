package security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.*
import java.util.Date

class TokenService(
    secret: String,
    private val issuer: String,
    private val audience: String,
    private val ttlMillis: Long = 7L * 24 * 3600 * 1000,
) {
    private val algorithm = Algorithm.HMAC256(secret)

    fun create(userId: Long): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.toString())
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + ttlMillis))
            .sign(algorithm)
    }

    companion object {
        fun from(config: ApplicationConfig) = TokenService(
            secret = config.property("jwt.secret").getString(),
            issuer = config.property("jwt.issuer").getString(),
            audience = config.property("jwt.audience").getString(),
        )
    }
}