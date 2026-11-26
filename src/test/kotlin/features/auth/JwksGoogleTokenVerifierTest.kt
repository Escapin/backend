package features.auth

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.NetworkException
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.coroutines.runBlocking
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JwksGoogleTokenVerifierTest {

    private val googleKey = newRsaKey()

    // Simule le JWKS de Google : une seule clé connue.
    private val jwkProvider = JwkProvider { kid ->
        if (kid == KID) googleKey.toJwk(kid) else throw SigningKeyNotFoundException("Unknown kid $kid", null)
    }

    private val verifier = JwksGoogleTokenVerifier(CLIENT_ID, jwkProvider)

    @Test
    fun `valid token gives the google identity`() = runBlocking {
        val identity = verifier.verify(token())

        assertEquals(
            GoogleIdentity(
                subject = SUBJECT,
                email = "jean.dupont@gmail.com",
                name = "Jean",
                surname = "Dupont",
                pictureUrl = "https://lh3.googleusercontent.com/photo.jpg",
            ),
            identity,
        )
    }

    @Test
    fun `both google issuers are accepted`() = runBlocking {
        assertEquals(SUBJECT, verifier.verify(token(issuer = "accounts.google.com"))?.subject)
    }

    @Test
    fun `unverified email is dropped`() = runBlocking {
        assertNull(verifier.verify(token(emailVerified = false))?.email)
    }

    @Test
    fun `token for another app is rejected`() = runBlocking {
        assertNull(verifier.verify(token(audience = "other-app.apps.googleusercontent.com")))
    }

    @Test
    fun `token not issued by google is rejected`() = runBlocking {
        assertNull(verifier.verify(token(issuer = "https://evil.example.com")))
    }

    @Test
    fun `expired token is rejected`() = runBlocking {
        assertNull(verifier.verify(token(expiresAt = Instant.now().minusSeconds(120))))
    }

    @Test
    fun `token signed with another key is rejected`() = runBlocking {
        assertNull(verifier.verify(token(signingKey = newRsaKey())))
    }

    @Test
    fun `token with an unknown key id is rejected`() = runBlocking {
        assertNull(verifier.verify(token(kid = "unknown")))
    }

    @Test
    fun `token signed with HMAC is rejected`() = runBlocking {
        val forged = JWT.create()
            .withKeyId(KID)
            .withIssuer("https://accounts.google.com")
            .withAudience(CLIENT_ID)
            .withSubject(SUBJECT)
            .withExpiresAt(Instant.now().plusSeconds(3600))
            .sign(Algorithm.HMAC256("secret"))

        assertNull(verifier.verify(forged))
    }

    @Test
    fun `malformed token is rejected`() = runBlocking {
        assertNull(verifier.verify("not-a-jwt"))
    }

    @Test
    fun `unreachable google keys are reported as unavailable`() {
        val offline = JwksGoogleTokenVerifier(CLIENT_ID) { throw NetworkException("offline", null) }

        assertFailsWith<GoogleUnavailableException> { runBlocking { offline.verify(token()) } }
    }

    @Test
    fun `blank client id fails fast`() {
        assertFailsWith<IllegalArgumentException> { JwksGoogleTokenVerifier(" ", jwkProvider) }
    }

    private fun token(
        audience: String = CLIENT_ID,
        issuer: String = "https://accounts.google.com",
        expiresAt: Instant = Instant.now().plusSeconds(3600),
        emailVerified: Boolean = true,
        kid: String = KID,
        signingKey: KeyPair = googleKey,
    ): String = JWT.create()
        .withKeyId(kid)
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(SUBJECT)
        .withClaim("email", "Jean.Dupont@Gmail.com")
        .withClaim("email_verified", emailVerified)
        .withClaim("given_name", "Jean")
        .withClaim("family_name", "Dupont")
        .withClaim("picture", "https://lh3.googleusercontent.com/photo.jpg")
        .withIssuedAt(expiresAt.minusSeconds(3600))
        .withExpiresAt(expiresAt)
        .sign(Algorithm.RSA256(signingKey.public as RSAPublicKey, signingKey.private as RSAPrivateKey))

    private companion object {
        const val CLIENT_ID = "123-escapin.apps.googleusercontent.com"
        const val SUBJECT = "109876543210"
        const val KID = "google-key-1"

        fun newRsaKey(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

        /** Clé publique au format JWK, comme dans https://www.googleapis.com/oauth2/v3/certs. */
        fun KeyPair.toJwk(kid: String): Jwk {
            val key = public as RSAPublicKey
            return Jwk.fromValues(
                mapOf(
                    "kid" to kid,
                    "kty" to "RSA",
                    "alg" to "RS256",
                    "use" to "sig",
                    "n" to key.modulus.toBase64Url(),
                    "e" to key.publicExponent.toBase64Url(),
                )
            )
        }

        fun BigInteger.toBase64Url(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray())
    }
}
