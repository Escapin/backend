package features.users

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

class UserRepository(private val database: R2dbcDatabase) {

    suspend fun existsByPseudo(pseudo: String): Boolean = TODO()

    suspend fun existsByEmail(email: String): Boolean = TODO()

    suspend fun create(
        pseudo: String,
        email: String,
        passwordHash: String,
        name: String?,
        surname: String?,
    ): Long = TODO()
}