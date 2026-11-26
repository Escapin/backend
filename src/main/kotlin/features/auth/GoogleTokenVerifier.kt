package features.auth

import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwk.NetworkException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.RSAKeyProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

data class GoogleIdentity(
    val subject: String,
    val email: String?,
    val name: String?,
    val surname: String?,
    val pictureUrl: String?,
)

fun interface GoogleTokenVerifier {
    /** `null` si le token est invalide : signature, émetteur, audience ou expiration. */
    suspend fun verify(idToken: String): GoogleIdentity?
}

class GoogleUnavailableException(cause: Throwable) : RuntimeException("Google signing keys unavailable", cause)
/**Vérifie l'ID token localement avec les clés publiques de Google (JWKS, mises en cache) si google injoignable.*/

class JwksGoogleTokenVerifier(
    webClientId: String,
    jwkProvider: JwkProvider = defaultJwkProvider(),
) : GoogleTokenVerifier {

    init {
        require(webClientId.isNotBlank()) { "Google web client id is missing (GOOGLE_WEB_CLIENT_ID)." }
    }

    // RS256 imposé : un token signé autrement (ou "alg: none") est refusé.
    private val verifier = JWT.require(Algorithm.RSA256(GoogleKeyProvider(jwkProvider)))
        .withIssuer(*ISSUERS)
        .withAudience(webClientId)
        .acceptLeeway(CLOCK_SKEW_SECONDS)
        .build()

    // Le 1er appel télécharge les clés : bloquant, donc hors des threads Ktor.
    override suspend fun verify(idToken: String): GoogleIdentity? = withContext(Dispatchers.IO) {
        try {
            verifier.verify(idToken).toIdentity()
        } catch (e: JWTVerificationException) {
            null
        }
    }

    private fun DecodedJWT.toIdentity(): GoogleIdentity? {
        val subject = subject?.takeIf { it.isNotBlank() } ?: return null
        val emailVerified = getClaim("email_verified").asBoolean() == true

        return GoogleIdentity(
            subject = subject,
            // E-mail non vérifié : n'importe qui pourrait le revendiquer.
            email = getClaim("email").asString()?.trim()?.lowercase()?.takeIf { emailVerified },
            name = getClaim("given_name").asString(),
            surname = getClaim("family_name").asString(),
            pictureUrl = getClaim("picture").asString(),
        )
    }

    /** Fournit la clé publique correspondant au `kid` du token. */
    private class GoogleKeyProvider(private val jwkProvider: JwkProvider) : RSAKeyProvider {

        override fun getPublicKeyById(keyId: String?): RSAPublicKey? {
            if (keyId == null) return null
            return try {
                jwkProvider.get(keyId).publicKey as? RSAPublicKey
            } catch (e: NetworkException) {
                throw GoogleUnavailableException(e)
            } catch (e: JwkException) {
                // Clé inconnue : la signature sera refusée.
                null
            } catch (e: RuntimeException) {
                // Réponse JWKS inattendue : panne côté Google, pas un token invalide.
                throw GoogleUnavailableException(e)
            }
        }

        // Vérification seulement : on ne signe jamais avec les clés de Google.
        override fun getPrivateKey(): RSAPrivateKey? = null

        override fun getPrivateKeyId(): String? = null
    }

    private companion object {
        const val CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs"
        val ISSUERS = arrayOf("accounts.google.com", "https://accounts.google.com")
        const val CLOCK_SKEW_SECONDS = 30L

        // Google change ses clés environ tous les jours : cache court + limite contre les kid bidons.
        fun defaultJwkProvider(): JwkProvider = JwkProviderBuilder(URI(CERTS_URL).toURL())
            .cached(10, 6, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
    }
}
