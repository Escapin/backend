package features.users

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val pseudo: String,
    val email: String,
    val password: String,
    val name: String? = null,
    val surname: String? = null,
    val cguAccepted: Boolean,
) {
    override fun toString() = "RegisterRequest(pseudo=$pseudo, email=<redacted>, password=<redacted>)"
}

@Serializable
data class UserResponse(
    val id: Long,
    val pseudo: String,
    val name: String? = null,
    val surname: String? = null,
)

data class NewUser(
    val pseudo: String,
    val email: String,
    val password: String,
    val name: String?,
    val surname: String?,
) {
    override fun toString() = "NewUser(pseudo=$pseudo, email=<redacted>, password=<redacted>)"
}


@Serializable
data class PseudoAvailabilityResponse(val pseudo: String, val available: Boolean)
