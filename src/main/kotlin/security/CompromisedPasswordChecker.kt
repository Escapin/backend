package security

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration

class CompromisedPasswordChecker(
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build()
) {
    private val log = LoggerFactory.getLogger(CompromisedPasswordChecker::class.java)

    private val localBlocklist: Set<String> by lazy { loadLocalBlocklist() }

    suspend fun isCompromised(password: String): Boolean {
        val sha1 = password.toSha1UpperCase()
        val prefix = sha1.substring(0, 5)
        val suffix = sha1.substring(5)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.pwnedpasswords.com/range/$prefix"))
            .header("Add-Padding", "true")
            .header("User-Agent", "Escapin-PasswordCheck")
            .timeout(Duration.ofSeconds(3))
            .GET()
            .build()

        return try {
            val response = withContext(Dispatchers.IO) {
                client.send(request, HttpResponse.BodyHandlers.ofString())
            }
            if (response.statusCode() != 200) {
                log.warn("Pwned Passwords returned HTTP ${response.statusCode()}, using offline check")
                return offlineCheck(password)
            }

            val target = "$suffix:"
            response.body().lineSequence().any { raw ->
                val line = raw.trim()
                line.startsWith(target, ignoreCase = true) && !line.endsWith(":0")
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("Pwned Passwords unreachable, using offline check: ${e.message}")
            offlineCheck(password)
        }
    }

    private fun String.toSha1UpperCase(): String {
        return MessageDigest.getInstance("SHA-1")
            .digest(this.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02X".format(it) }
    }

    private fun offlineCheck(password: String): Boolean {
        val lower = password.lowercase()

        if (lower in localBlocklist) return true
        if (lower.toSet().size <= 3) return true

        return false
    }

    private fun loadLocalBlocklist(): Set<String> {
        val stream = CompromisedPasswordChecker::class.java.getResourceAsStream("/common-passwords.txt")
        if (stream == null) {
            log.warn("common-passwords.txt not found: local blocklist is empty")
            return emptySet()
        }

        return stream.bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toHashSet()
        }
    }
}