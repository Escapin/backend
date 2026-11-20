package database.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object Pins : LongIdTable("pin") {
    val idUser = reference("id_user", Users)
    val idVoyage = reference("id_voyage", Voyages).nullable()
    val titre = text("titre")
    val descriptionPin = text("description_pin").nullable()
    val lieux = text("lieux").nullable()
    val countryCode = text("country_code").nullable()
    val latitude = double("latitude")
    val longitude = double("longitude")

    val datePin = date("date_pin").databaseGenerated()

    val visibilite = customEnumeration(
        name = "visibilite",
        sql = "visibilite_type",
        fromDb = { Visibilite.fromDb(it as String) },
        toDb = { it.db },
    )
    val favori = bool("favori").default(false)
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
}
