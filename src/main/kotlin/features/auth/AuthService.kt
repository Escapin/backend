package features.auth

import common.ApiException
import database.isUniqueViolation
import database.tables.ProviderType
import features.users.UserRepository
import io.ktor.http.*
import security.TokenService
import kotlin.random.Random

class AuthService(
    private val userRepository: UserRepository,
    private val githubClient: GithubClient,
    private val tokenService: TokenService,
) {
    suspend fun loginWithGithub(githubAccessToken: String): String {
        val gh = try {
            githubClient.fetchUser(githubAccessToken)
        } catch (_: GithubUnavailableException) {
            throw ApiException(HttpStatusCode.BadGateway, "github_unavailable", "GitHub is unavailable.")
        }
        val uid = gh.id.toString()

        val userId = userRepository.findIdByProvider(ProviderType.GITHUB, uid)
            ?: createGithubUser(gh, uid, githubAccessToken)

        return tokenService.create(userId)
    }

    private suspend fun createGithubUser(gh: GithubUser, uid: String, accessToken: String): Long {
        var email = (gh.email ?: githubClient.fetchPrimaryEmail(accessToken))?.trim()?.lowercase()
        if (email != null && userRepository.existsByEmail(email)) email = null

        val base = toPseudo(gh.login)
        for (attempt in 0 until 5) {
            val pseudo = if (attempt == 0) base else suffixed(base)
            if (attempt == 0 && userRepository.existsByPseudo(pseudo)) continue

            try {
                return userRepository.createFromProvider(
                    provider = ProviderType.GITHUB,
                    providerUid = uid,
                    pseudo = pseudo,
                    email = email,
                    name = gh.name?.take(50),
                    avatarUrl = gh.avatarUrl,
                )
            } catch (e: Exception) {
                if (!e.isUniqueViolation()) throw e
                userRepository.findIdByProvider(ProviderType.GITHUB, uid)?.let { return it }
            }
        }
        throw ApiException(HttpStatusCode.Conflict, "pseudo_unavailable", "Could not generate a unique pseudo.")
    }

    private fun toPseudo(login: String): String {
        var p = login.lowercase().replace(Regex("[^a-z0-9_.]"), "_").take(25)
        if (p.length < 3) p = p.padEnd(3, '_')
        if (p in RESERVED) p += "_"
        return p
    }

    private fun suffixed(base: String) = base.take(25) + "_" + Random.nextInt(1000, 9999)

    private companion object {
        val RESERVED = setOf("admin", "administrator", "root", "support", "system", "moderator", "escapin", "null", "undefined")
    }
}