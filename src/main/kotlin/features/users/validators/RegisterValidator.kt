package features.users.validators

import common.FieldError
import common.ValidationException
import features.users.NewUser
import features.users.RegisterRequest

object RegisterValidator {
    const val PASSWORD_MIN_LENGTH = 8
    const val PASSWORD_MAX_LENGTH = 128

    private val PSEUDO_REGEX = Regex("^[a-z0-9_.]{3,30}$")
    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val CONTROL_CHARS = Regex("\\p{Cc}")
    private const val EMAIL_MAX_LENGTH = 254
    private const val NAME_MAX_LENGTH = 50

    private val RESERVED_PSEUDOS = setOf(
        "admin", "administrator", "root", "support", "system", "moderator", "escapin", "null", "undefined",
    )

    fun validate(request: RegisterRequest): NewUser {
        val errors = mutableListOf<FieldError>()

        val pseudo = request.pseudo.trim()
        pseudoError(pseudo)?.let { errors += it }

        val email = request.email.trim().lowercase()
        if (email.length > EMAIL_MAX_LENGTH || !EMAIL_REGEX.matches(email)) {
            errors += FieldError("email", "invalid_format", "Email address is not valid.")
        }

        val passwordLength = request.password.codePointCount(0, request.password.length)
        if (request.password.isBlank()) {
            errors += FieldError("password", "required", "Password is required.")
        }
        if (passwordLength < PASSWORD_MIN_LENGTH) {
            errors += FieldError("password", "too_short", "Password must be at least $PASSWORD_MIN_LENGTH characters. A passphrase works well.")
        }
        if (passwordLength > PASSWORD_MAX_LENGTH) {
            errors += FieldError("password", "too_long", "Password must be at most $PASSWORD_MAX_LENGTH characters.")
        }

        val name = cleanName("name", request.name, errors)
        val surname = cleanName("surname", request.surname, errors)

        if (!request.cguAccepted) {
            errors += FieldError("cguAccepted", "must_be_accepted", "You must accept the terms of use.")
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
        return NewUser(pseudo, email, request.password, name, surname)
    }

    private fun cleanName(field: String, value: String?, errors: MutableList<FieldError>): String? {
        val cleaned = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (cleaned.length > NAME_MAX_LENGTH || CONTROL_CHARS.containsMatchIn(cleaned)) {
            errors += FieldError(field, "invalid_format", "Must be at most $NAME_MAX_LENGTH characters, without control characters.")
            return null
        }
        return cleaned
    }

    fun pseudoError(pseudo: String): FieldError? {
        if (!PSEUDO_REGEX.matches(pseudo)) {
            return FieldError("pseudo", "invalid_format", "Pseudo must be 3-30 characters: letters, digits, '_' or '.'.")
        }

        if (pseudo.lowercase() in RESERVED_PSEUDOS) {
            return FieldError("pseudo", "reserved", "This pseudo is not available.")
        }

        return null
    }
}