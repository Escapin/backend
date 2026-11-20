package database.tables

import org.jetbrains.exposed.v1.core.Table

object PinParticipants : Table("pin_participant") {
    val idPin = reference("id_pin", Pins)
    val idUser = reference("id_user", Users)

    override val primaryKey = PrimaryKey(idPin, idUser)
}
