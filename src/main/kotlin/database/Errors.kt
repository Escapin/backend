package database

import io.r2dbc.spi.R2dbcException

private const val UNIQUE_VIOLATION = "23505"

// Classe SQLSTATE 08 = erreur de connexion (base éteinte, refusée, coupée…).
private const val CONNECTION_EXCEPTION_CLASS = "08"

fun Throwable.isUniqueViolation(): Boolean =
    generateSequence(this) { it.cause }.any { it is R2dbcException && it.sqlState == UNIQUE_VIOLATION }

/** Parcourt l'exception et ses causes : vrai si la base n'a pas pu être jointe. */
fun Throwable.isConnectionFailure(): Boolean =
    generateSequence(this) { it.cause }.any { it is R2dbcException && it.sqlState?.startsWith(CONNECTION_EXCEPTION_CLASS) == true }
