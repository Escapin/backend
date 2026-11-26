package features.auth

import features.users.UserResponse
import kotlinx.serialization.Serializable

/** 1er appel : [idToken] seul. Si `registration_required`, le client renvoie le même token + [pseudo] + [cguAccepted]. */
@Serializable
data class GoogleAuthRequest(
    val idToken: String,
    val pseudo: String? = null,
    val cguAccepted: Boolean? = null,
) {
    override fun toString() = "GoogleAuthRequest(pseudo=$pseudo, cguAccepted=$cguAccepted, idToken=<redacted>)"
}

@Serializable
data class AuthResponse(
    val accessToken: String,
    val user: UserResponse,
) {
    override fun toString() = "AuthResponse(user=$user, accessToken=<redacted>)"
}
