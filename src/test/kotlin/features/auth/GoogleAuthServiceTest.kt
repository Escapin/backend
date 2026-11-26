package features.auth

import com.auth0.jwt.JWT
import common.ApiException
import common.ValidationException
import database.tables.ProviderType
import features.users.UserRepository
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.junit.Assume.assumeTrue
import security.TokenService
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests d'intégration sur une vraie base PostgreSQL (schéma de database/init.sql).
 * Ignorés si TEST_DB_URL n'est pas défini, ex. : r2dbc:postgresql://postgres:test@localhost:5433/escapin
 * Chaque test utilise des données uniques : rien n'est supprimé.
 */
class GoogleAuthServiceTest {

    private val identities = mutableMapOf<String, GoogleIdentity>()
    private var googleDown = false

    // Faux Google : renvoie l'identité associée au token, null sinon.
    private val verifier = GoogleTokenVerifier { idToken ->
        if (googleDown) throw GoogleUnavailableException(RuntimeException("offline"))
        identities[idToken]
    }

    private lateinit var userRepository: UserRepository
    private lateinit var service: GoogleAuthService

    @BeforeTest
    fun setUp() {
        val url = System.getenv("TEST_DB_URL")
        assumeTrue("TEST_DB_URL not set: database tests skipped", !url.isNullOrBlank())

        userRepository = UserRepository(R2dbcDatabase.connect(url = url!!))
        service = GoogleAuthService(userRepository, verifier, TokenService("test-secret", "test-issuer", "test-audience"))
    }

    @Test
    fun `unknown google account asks for registration and creates nothing`() = runBlocking {
        val google = newGoogleUser()

        val error = assertFailsWith<ApiException> { service.login(GoogleAuthRequest(google.token)) }

        assertEquals(HttpStatusCode.NotFound, error.status)
        assertEquals("registration_required", error.code)
        assertNull(userRepository.findUserByProvider(ProviderType.GOOGLE, google.identity.subject))
    }

    @Test
    fun `registration creates the account then the same google account logs in`() = runBlocking {
        val google = newGoogleUser()
        val pseudo = uniquePseudo()

        val registered = service.login(GoogleAuthRequest(google.token, pseudo = "  $pseudo ", cguAccepted = true))
        val loggedIn = service.login(GoogleAuthRequest(google.token))

        assertEquals(pseudo, registered.user.pseudo)
        assertEquals("Jean", registered.user.name)
        assertEquals(registered.user, loggedIn.user)
        assertEquals(registered.user.id.toString(), JWT.decode(loggedIn.accessToken).subject)
    }

    @Test
    fun `invalid pseudo and missing cgu are both reported`() = runBlocking {
        val google = newGoogleUser()

        val error = assertFailsWith<ValidationException> {
            service.login(GoogleAuthRequest(google.token, pseudo = "No", cguAccepted = false))
        }

        assertEquals(listOf("pseudo" to "invalid_format", "cguAccepted" to "must_be_accepted"), error.fieldCodes())
    }

    @Test
    fun `reserved pseudo is rejected`() = runBlocking {
        val google = newGoogleUser()

        val error = assertFailsWith<ValidationException> {
            service.login(GoogleAuthRequest(google.token, pseudo = "admin", cguAccepted = true))
        }

        assertEquals(listOf("pseudo" to "reserved"), error.fieldCodes())
    }

    @Test
    fun `pseudo already taken is a conflict`() = runBlocking {
        val pseudo = uniquePseudo()
        service.login(GoogleAuthRequest(newGoogleUser().token, pseudo = pseudo, cguAccepted = true))

        val error = assertFailsWith<ApiException> {
            service.login(GoogleAuthRequest(newGoogleUser().token, pseudo = pseudo, cguAccepted = true))
        }

        assertEquals("pseudo_taken", error.code)
    }

    @Test
    fun `email of a password account is never linked automatically`() = runBlocking {
        val google = newGoogleUser()
        userRepository.create(uniquePseudo(), google.identity.email!!, "argon2-hash", null, null)

        val error = assertFailsWith<ApiException> { service.login(GoogleAuthRequest(google.token)) }

        assertEquals(HttpStatusCode.Conflict, error.status)
        assertEquals("email_taken", error.code)
    }

    @Test
    fun `unverified google email does not block registration`() = runBlocking {
        val google = newGoogleUser(email = null)

        val response = service.login(GoogleAuthRequest(google.token, pseudo = uniquePseudo(), cguAccepted = true))

        assertNotNull(userRepository.findUserByProvider(ProviderType.GOOGLE, google.identity.subject))
        assertEquals(response.user, userRepository.findUserByProvider(ProviderType.GOOGLE, google.identity.subject))
    }

    @Test
    fun `google profile names are cleaned before storage`() = runBlocking {
        val google = newGoogleUser(name = "  Je\u0000an  ", surname = "D".repeat(80))

        val user = service.login(GoogleAuthRequest(google.token, pseudo = uniquePseudo(), cguAccepted = true)).user

        assertEquals("Jean", user.name)
        assertEquals(50, user.surname?.length)
    }

    @Test
    fun `invalid google token is unauthorized`() = runBlocking {
        val error = assertFailsWith<ApiException> { service.login(GoogleAuthRequest("forged-token")) }

        assertEquals(HttpStatusCode.Unauthorized, error.status)
        assertEquals("invalid_google_token", error.code)
    }

    @Test
    fun `google outage is a service unavailable`() = runBlocking {
        googleDown = true

        val error = assertFailsWith<ApiException> { service.login(GoogleAuthRequest(newGoogleUser().token)) }

        assertEquals(HttpStatusCode.ServiceUnavailable, error.status)
        assertEquals("google_unavailable", error.code)
    }

    private data class GoogleUser(val token: String, val identity: GoogleIdentity)

    private fun newGoogleUser(
        email: String? = "${uniqueId()}@gmail.com",
        name: String? = "Jean",
        surname: String? = "Dupont",
    ): GoogleUser {
        val identity = GoogleIdentity(
            subject = uniqueId(),
            email = email,
            name = name,
            surname = surname,
            pictureUrl = null,
        )
        val token = "token-${identity.subject}"
        identities[token] = identity
        return GoogleUser(token, identity)
    }

    private fun uniqueId() = UUID.randomUUID().toString().replace("-", "").take(16)

    private fun uniquePseudo() = "g_${uniqueId()}"

    private fun ValidationException.fieldCodes() = errors.orEmpty().map { it.field to it.code }
}
