package security

import de.mkammerer.argon2.Argon2Factory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

class PasswordHasher {
    private val argon2 = Argon2Factory.create(
        Argon2Factory.Argon2Types.ARGON2id,
        16, 32
    )

    suspend fun hash(password: String): String = withContext(Dispatchers.Default) {
        val chars = normalize(password)
        try {
            argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, chars)
        } finally {
            argon2.wipeArray(chars)
        }
    }

    suspend fun verify(password: String, stored: String): Boolean = withContext(Dispatchers.Default) {
        val chars = normalize(password)
        try {
            argon2.verify(stored, chars)
        } finally {
            argon2.wipeArray(chars)
        }
    }

    private fun normalize(password: String): CharArray =
        Normalizer.normalize(password, Normalizer.Form.NFKC).toCharArray()

    private companion object {
        const val ITERATIONS = 2
        const val MEMORY_KIB = 19_456 // 19 MiB
        const val PARALLELISM = 1
    }
}