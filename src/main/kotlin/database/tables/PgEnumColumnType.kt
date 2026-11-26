package database.tables

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

/**
 * Colonne d'un type ENUM PostgreSQL.
 * R2DBC envoie les String en `varchar` : sans cast, Postgres refuse l'INSERT et la comparaison `=`.
 */
class PgEnumColumnType<T : Enum<T>>(
    private val sqlTypeName: String,
    private val fromDb: (Any) -> T,
    private val toDb: (T) -> String,
) : ColumnType<T>() {

    override fun sqlType() = sqlTypeName

    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): T = if (value is Enum<*>) value as T else fromDb(value)

    override fun notNullValueToDB(value: T): Any = toDb(value)

    override fun nonNullValueToString(value: T) = "'${toDb(value)}'"

    // Ex. `?::provider_type` : le paramètre est converti dans le type enum côté Postgres.
    override fun parameterMarker(value: T?) = "?::$sqlTypeName"
}

fun <T : Enum<T>> Table.pgEnumeration(
    name: String,
    sqlTypeName: String,
    fromDb: (Any) -> T,
    toDb: (T) -> String,
): Column<T> = registerColumn(name, PgEnumColumnType(sqlTypeName, fromDb, toDb))
