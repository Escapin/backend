package database

import io.r2dbc.spi.R2dbcException

private const val UNIQUE_VIOLATION = "23505"

fun Throwable.isUniqueViolation(): Boolean =
    generateSequence(this) { it.cause }.any { it is R2dbcException && it.sqlState == UNIQUE_VIOLATION }