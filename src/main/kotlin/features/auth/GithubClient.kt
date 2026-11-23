package features.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubUser(
    val id: Long,
    val login: String,
    val name: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class GithubEmail(
    val email: String,
    val primary: Boolean,
    val verified: Boolean
)

class GithubUnavailableException(message: String) : RuntimeException(message)


class GithubClient(private val http: HttpClient) {
    suspend fun fetchUser(accessToken: String): GithubUser {
        val response = http.get("https://api.github.com/user") {
            bearerAuth(accessToken)
            header(HttpHeaders.Accept, "application/vnd.github+json")
        }

        if (!response.status.isSuccess()) throw GithubUnavailableException("GitHub /user: ${response.status}")
        return response.body()
    }

    suspend fun fetchPrimaryEmail(accessToken: String): String? {
        val response = http.get("https://api.github.com/user/emails") {
            bearerAuth(accessToken)
            header(HttpHeaders.Accept, "application/vnd.github+json")
        }

        if (!response.status.isSuccess()) return null
        val emails: List<GithubEmail> = response.body()
        return emails.firstOrNull { it.primary && it.verified }?.email
    }
}