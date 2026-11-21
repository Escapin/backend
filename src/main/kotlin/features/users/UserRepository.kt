package features.users

import database.tables.Users
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.time.OffsetDateTime
import java.time.ZoneOffset

class UserRepository(private val database: R2dbcDatabase) {
    suspend fun existsByPseudo(pseudo: String): Boolean = suspendTransaction(database) {
        Users.selectAll().where {
            Users.pseudo.lowerCase() eq pseudo.lowercase()
        }.count() > 0
    }

    suspend fun existsByEmail(email: String): Boolean = suspendTransaction(database) {
        Users.selectAll().where {
            Users.email.lowerCase() eq email.lowercase()
        }.count() > 0
    }

    suspend fun create(
        pseudo: String,
        email: String,
        passwordHash: String,
        name: String?,
        surname: String?,
    ): UserResponse = suspendTransaction(database) {
        val inserted = Users.insert {
            it[Users.pseudo] = pseudo
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.name] = name
            it[Users.surname] = surname
            it[Users.cguAcceptedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        UserResponse(id = inserted[Users.id].value, pseudo = pseudo, name = name, surname = surname)
    }
}