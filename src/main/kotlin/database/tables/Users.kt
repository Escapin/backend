package database.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object Users : LongIdTable("users") {
    val pseudo = text("pseudo")
    val email = text("email").nullable()
    val passwordHash = text("password_hash").nullable()
    val name = text("name").nullable()
    val surname = text("surname").nullable()
    val villeOrigine = text("ville_origine").nullable()
    val urlPhotoProfil = text("url_photo_profil").nullable()
    val urlPhotoFondProfil = text("url_photo_fond_profil").nullable()
    val cguAcceptedAt = timestampWithTimeZone("cgu_accepted_at")
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
}
