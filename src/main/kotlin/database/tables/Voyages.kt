package database.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.date

object Voyages : LongIdTable("voyage") {
    val idUser = reference("id_user", Users)
    val titre = text("titre")
    val dateDebut = date("date_debut").nullable()
    val dateFin = date("date_fin").nullable()

    val idImageCover = long("id_image_cover").nullable()
}
