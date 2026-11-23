package features.users

import database.tables.AuthProviders
import database.tables.ProviderType
import database.tables.Users
import kotlinx.coroutines.flow.toList
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
        val userId = inserted[Users.id]

        AuthProviders.insert {
            it[AuthProviders.idUser] = userId
            it[AuthProviders.provider] = ProviderType.US
            it[AuthProviders.providerUid] = userId.value.toString()
        }

        UserResponse(id = userId.value, pseudo = pseudo, name = name, surname = surname)
    }

    suspend fun findIdByProvider(provider: ProviderType, providerUid: String): Long? =
        suspendTransaction(database) {
            AuthProviders.selectAll()
                .where { AuthProviders.providerUid eq providerUid }
                .toList()
                .firstOrNull { it[AuthProviders.provider] == provider }
                ?.get(AuthProviders.idUser)
                ?.value
        }

    suspend fun createFromProvider(
        provider: ProviderType,
        providerUid: String,
        pseudo: String,
        email: String?,
        name: String?,
        avatarUrl: String?,
    ): Long = suspendTransaction(database) {
        val inserted = Users.insert {
            it[Users.pseudo] = pseudo
            it[Users.email] = email
            it[Users.name] = name
            it[Users.urlPhotoProfil] = avatarUrl
            it[Users.cguAcceptedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        val userId = inserted[Users.id]

        AuthProviders.insert {
            it[AuthProviders.idUser] = userId
            it[AuthProviders.provider] = provider
            it[AuthProviders.providerUid] = providerUid
        }
        userId.value
    }

    suspend fun findProfileById(id: Long): MeResponse? = suspendTransaction(database) {
        Users.selectAll()
            .where { Users.id eq id }
            .toList()
            .firstOrNull()
            ?.let {
                MeResponse(
                    id = it[Users.id].value,
                    pseudo = it[Users.pseudo],
                    email = it[Users.email],
                    name = it[Users.name],
                    surname = it[Users.surname],
                    avatarUrl = it[Users.urlPhotoProfil],
                )
            }
    }
}