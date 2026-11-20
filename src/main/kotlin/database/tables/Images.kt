package database.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object Images : LongIdTable("images") {
    val idPin = reference("id_pin", Pins)
    val pictureUrl = text("picture_url")
    val position = short("position").default(0)
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
}
