package database.tables

enum class Visibilite(val db: String) {
    FRIENDS("friends"),
    PRIVATE("private"),
    PUBLIC("public");

    companion object {
        fun fromDb(value: String) = entries.first { it.db == value }
    }
}

enum class ProviderType(val db: String) {
    GOOGLE("google"),
    GITHUB("github"),
    APPLE("apple"),
    US("us");

    companion object {
        fun fromDb(value: String) = entries.first { it.db == value }
    }
}
