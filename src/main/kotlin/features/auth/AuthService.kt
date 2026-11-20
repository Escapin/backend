package features.auth

import features.users.UserRepository

class AuthService(private val userRepository: UserRepository) {

    suspend fun register(request: RegisterRequest): RegisterResponse {
        // TODO: valider les champs (pseudo, email, mot de passe, cguAccepted)
        // TODO: vérifier que le pseudo et l'email ne sont pas déjà pris
        // TODO: hasher le mot de passe
        // TODO: créer l'utilisateur via userRepository
        TODO("register not implemented")
    }
}