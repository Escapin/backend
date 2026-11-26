package database.tables

import org.jetbrains.exposed.v1.core.Table

object AuthProviders : Table("auth_provider") {
    val idUser = reference("id_user", Users)

    val provider = pgEnumeration(
        name = "provider",
        sqlTypeName = "provider_type",
        fromDb = { ProviderType.fromDb(it as String) },
        toDb = { it.db },
    )
    val providerUid = text("provider_uid")

    override val primaryKey = PrimaryKey(provider, providerUid)
}
