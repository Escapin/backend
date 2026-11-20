package features.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val pseudo: String,
    val email: String,
    val password: String,
    val name: String? = null,
    val surname: String? = null,
    val cguAccepted: Boolean,
)

@Serializable
data class RegisterResponse(
    val id: Long,
    val pseudo: String,
)