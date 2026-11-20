package database.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object ListeAmis : Table("liste_amis") {
    val idUser = reference("id_user", Users)
    val idFriend = reference("id_friend", Users)
    val status = varchar("status", 8)
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()

    override val primaryKey = PrimaryKey(idUser, idFriend)
}
