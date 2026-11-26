package features.auth

import common.ApiException
import common.ConflictException
import common.FieldError
import common.ValidationException
import database.isUniqueViolation
import database.tables.ProviderType
import features.users.UserRepository
import features.users.UserResponse
import features.users.validators.RegisterValidator
import io.ktor.http.*
import security.TokenService

/**
 * Connexion Google : l'app envoie un ID token, on renvoie un JWT Escapin.
 * Compte inconnu → `registration_required`, puis l'app rappelle avec pseudo + CGU.
 */
class  GoogleAuthService(
    private val userRepository: UserRepository,
    private val tokenVerifier: GoogleTokenVerifier,
    private val tokenService: TokenService,
) {
    suspend fun login(request: GoogleAuthRequest): AuthResponse {
        val identity = verify(request.idToken)

        // Compte déjà lié à ce Google : connexion directe.
        userRepository.findUserByProvider(ProviderType.GOOGLE, identity.subject)?.let { return authResponse(it) }

        // Vérifié avant le pseudo : inutile de faire remplir un formulaire voué à échouer.
        // Pas de liaison auto : l'e-mail d'un compte mot de passe n'est jamais vérifié.
        if (isEmailUsed(identity.email)) throw emailTaken()

        val rawPseudo = request.pseudo ?: throw ApiException(
            HttpStatusCode.NotFound,
            "registration_required",
            "No account is linked to this Google identity. Send a pseudo and accept the terms of use.",
        )
        val pseudo = validateRegistration(rawPseudo, request.cguAccepted)
        if (userRepository.existsByPseudo(pseudo)) throw pseudoTaken()

        return authResponse(createUser(identity, pseudo))
    }

    private suspend fun verify(idToken: String): GoogleIdentity {
        val identity = try {
            tokenVerifier.verify(idToken)
        } catch (e: GoogleUnavailableException) {
            throw ApiException(HttpStatusCode.ServiceUnavailable, "google_unavailable", "Google is unavailable.")
        }
        return identity ?: throw ApiException(
            HttpStatusCode.Unauthorized,
            "invalid_google_token",
            "The Google ID token is invalid or expired.",
        )
    }

    private fun validateRegistration(rawPseudo: String, cguAccepted: Boolean?): String {
        val pseudo = rawPseudo.trim()
        val errors = listOfNotNull(
            RegisterValidator.pseudoError(pseudo),
            // Ajoutée seulement si les CGU ne sont pas acceptées.
            FieldError("cguAccepted", "must_be_accepted", "You must accept the terms of use.")
                .takeIf { cguAccepted != true },
        )
        if (errors.isNotEmpty()) throw ValidationException(errors)
        return pseudo
    }

    private suspend fun createUser(identity: GoogleIdentity, pseudo: String): UserResponse {
        val name = cleanProfileName(identity.name)
        val surname = cleanProfileName(identity.surname)

        return try {
            val id = userRepository.createFromProvider(
                provider = ProviderType.GOOGLE,
                providerUid = identity.subject,
                pseudo = pseudo,
                email = identity.email,
                name = name,
                surname = surname,
                avatarUrl = identity.pictureUrl,
            )
            UserResponse(id = id, pseudo = pseudo, name = name, surname = surname)
        } catch (e: Exception) {
            if (!e.isUniqueViolation()) throw e
            // Deux requêtes simultanées : si l'autre a créé ce compte Google, on le renvoie.
            userRepository.findUserByProvider(ProviderType.GOOGLE, identity.subject)
                ?: throw if (isEmailUsed(identity.email)) emailTaken() else pseudoTaken()
        }
    }

    private suspend fun isEmailUsed(email: String?) = email != null && userRepository.existsByEmail(email)

    private fun authResponse(user: UserResponse) = AuthResponse(tokenService.create(user.id), user)

    private fun cleanProfileName(value: String?): String? =
        value?.filterNot(Char::isISOControl)?.trim()?.take(NAME_MAX_LENGTH)?.ifEmpty { null }

    private fun emailTaken() = ConflictException("email_taken", "An account with this email already exists.")

    private fun pseudoTaken() = ConflictException("pseudo_taken", "This pseudo is already taken.")

    private companion object {
        const val NAME_MAX_LENGTH = 50
    }
}
