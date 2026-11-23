package features.users

import common.ConflictException
import common.FieldError
import common.ValidationException
import database.isUniqueViolation
import features.users.validators.RegisterValidator
import security.CompromisedPasswordChecker
import security.PasswordHasher

class UserService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val compromisedPasswordChecker: CompromisedPasswordChecker
) {
    suspend fun register(request: RegisterRequest): UserResponse {
        val newUser = RegisterValidator.validate(request)

        if (userRepository.existsByPseudo(newUser.pseudo)) {
            throw ConflictException("pseudo_taken", "This pseudo is already taken.")
        }
        if (userRepository.existsByEmail(newUser.email)) {
            throw ConflictException("email_taken", "An account with this email already exists.")
        }

        if (compromisedPasswordChecker.isCompromised(newUser.password)) {
            throw ConflictException("compromised_password", "This password has been compromised in a data breach. Please choose a different password.")
        }

        val passwordHash = passwordHasher.hash(newUser.password)

        return try {
            userRepository.create(
                pseudo = newUser.pseudo,
                email = newUser.email,
                passwordHash = passwordHash,
                name = newUser.name,
                surname = newUser.surname,
            )

        } catch (e: Exception) {
            if (e.isUniqueViolation()) {
                throw ConflictException("account_exists", "An account with this pseudo or email already exists.")
            }
            throw e
        }
    }


    suspend fun isPseudoAvailable(rawPseudo: String?): PseudoAvailabilityResponse {
        val pseudo = rawPseudo?.trim().orEmpty()
        if (pseudo.isEmpty()) {
            throw ValidationException(listOf(FieldError(
                "pseudo", "required", "Query parameter 'pseudo' is required."
            )))
        }
        RegisterValidator.pseudoError(pseudo)?.let { throw ValidationException(listOf(it)) }

        return PseudoAvailabilityResponse(pseudo, available = !userRepository.existsByPseudo(pseudo))
    }


    suspend fun getProfile(userId: Long): MeResponse? = userRepository.findProfileById(userId)
}
