package plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.StatelessHmacNonceManager

fun Application.configureSecurity(httpClient: HttpClient) {
    val config = environment.config
    val secret = config.property("jwt.secret").getString()
    val issuer = config.property("jwt.issuer").getString()
    val audience = config.property("jwt.audience").getString()
    val jwtRealm = config.property("jwt.realm").getString()

    val githubClientId = config.property("OAuth2.github.client-id").getString()
    val githubClientSecret = config.property("OAuth2.github.client-secret").getString()
    val githubRedirectUri = config.property("OAuth2.github.redirect-uri").getString()

    authentication {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(audience)) JWTPrincipal(credential.payload) else null
            }
        }

        oauth("github") {
            urlProvider = { githubRedirectUri }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "github",
                    authorizeUrl = "https://github.com/login/oauth/authorize",
                    accessTokenUrl = "https://github.com/login/oauth/access_token",
                    clientId = githubClientId,
                    clientSecret = githubClientSecret,
                    defaultScopes = listOf("read:user", "user:email"),
                    requestMethod = HttpMethod.Post,
                    nonceManager = StatelessHmacNonceManager(secret.toByteArray(), timeoutMillis = 600_000),
                )
            }
            client = httpClient
        }
    }
}