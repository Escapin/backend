package features.auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.NetworkException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import features.users.UserRepository
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import plugins.configureSerialization
import plugins.configureStatusPages
import security.TokenService
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/** Codes HTTP de `POST /auth/google` quand Google ou la base sont en panne. Aucune base ni réseau requis. */
class GoogleAuthRouteErrorsTest {

    // Port 1 : connexion refusée immédiatement, comme quand database_dev est éteint.
    private val unreachableDatabase = UserRepository(R2dbcDatabase.connect(url = "r2dbc:postgresql://u:p@localhost:1/escapin"))

    private val validIdentity = GoogleTokenVerifier { GoogleIdentity("109876543210", "jean@gmail.com", "Jean", null, null) }

    @Test
    fun `valid google token with the database down is a 503, not a 500`() =
        assertProblem(validIdentity, HttpStatusCode.ServiceUnavailable, "database_unavailable")

    @Test
    fun `rejected google token is a 401`() =
        assertProblem({ null }, HttpStatusCode.Unauthorized, "invalid_google_token")

    @Test
    fun `unreachable google keys are a 503`() =
        assertProblem(
            JwksGoogleTokenVerifier(CLIENT_ID) { throw NetworkException("offline", null) },
            HttpStatusCode.ServiceUnavailable,
            "google_unavailable",
        )

    @Test
    fun `unexpected failure while fetching google keys is a 503, not a 500`() =
        assertProblem(
            JwksGoogleTokenVerifier(CLIENT_ID, JwkProvider { throw RuntimeException("unexpected JWKS payload") }),
            HttpStatusCode.ServiceUnavailable,
            "google_unavailable",
        )

    private fun assertProblem(verifier: GoogleTokenVerifier, status: HttpStatusCode, code: String) = testApplication {
        // Config vide : ne charge pas application.yaml (qui exige les variables d'environnement).
        environment { config = MapApplicationConfig() }
        application {
            configureSerialization()
            configureStatusPages()
            routing {
                googleAuthRoute(GoogleAuthService(unreachableDatabase, verifier, TokenService("test", "test", "test")))
            }
        }

        val response = client.post("/google") {
            contentType(ContentType.Application.Json)
            setBody("""{"idToken":"${signedToken()}"}""")
        }

        assertEquals(status, response.status)
        assertEquals(code, Json.parseToJsonElement(response.bodyAsText()).jsonObject["code"]?.jsonPrimitive?.content)
    }

    private companion object {
        const val CLIENT_ID = "123-escapin.apps.googleusercontent.com"

        /** Token RS256 bien formé : force le verifier à aller chercher la clé `kid`. */
        fun signedToken(): String {
            val key = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            return JWT.create()
                .withKeyId("google-key-1")
                .withIssuer("https://accounts.google.com")
                .withAudience(CLIENT_ID)
                .withSubject("109876543210")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.RSA256(key.public as RSAPublicKey, key.private as RSAPrivateKey))
        }
    }
}
