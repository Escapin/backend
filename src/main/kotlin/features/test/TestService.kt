package features.test

import database.tables.Users
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class TestService(private val database: R2dbcDatabase) {

    suspend fun countUsers(): Long = suspendTransaction(database) {
        Users.selectAll().count()
    }
}
