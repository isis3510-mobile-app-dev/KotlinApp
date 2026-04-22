package com.example.petcare.util

enum class InputFieldPolicy {
    GENERAL_TEXT,
    EMAIL,
    PASSWORD,
    PHONE,
    DECIMAL
}

data class SanitizedInput(
    val value: String,
    val rejectionMessage: String? = null
)

private val emailAllowedChars = Regex("[A-Za-z0-9@._+\\-]")
private val decimalRegex = Regex("""\d+(\.\d+)?""")

fun sanitizeForEditing(
    raw: String,
    fieldPolicy: InputFieldPolicy,
    maxLength: Int? = null,
    maxNumericValue: Double? = null
): SanitizedInput {
    val builder = StringBuilder()
    var rejectionMessage: String? = null
    var seenDecimalSeparator = false

    fun reject(message: String) {
        if (rejectionMessage == null) {
            rejectionMessage = message
        }
    }

    raw.codePoints().forEach { codePoint ->
        when (fieldPolicy) {
            InputFieldPolicy.GENERAL_TEXT -> {
                when {
                    isRejectedControlCharacter(codePoint) ->
                        reject("Line breaks, tabs, and control characters are not allowed.")
                    isEmojiCodePoint(codePoint) ->
                        reject("Emojis are not allowed.")
                    else -> builder.appendCodePoint(codePoint)
                }
            }

            InputFieldPolicy.EMAIL -> {
                when {
                    Character.isWhitespace(codePoint) ->
                        reject("Email cannot contain spaces.")
                    isRejectedControlCharacter(codePoint) ->
                        reject("Email cannot contain control characters.")
                    isEmojiCodePoint(codePoint) ->
                        reject("Emojis are not allowed in email.")
                    emailAllowedChars.matches(codePoint.asString()) ->
                        builder.appendCodePoint(codePoint)
                    else -> reject("Email can only contain letters, numbers, and @._+-")
                }
            }

            InputFieldPolicy.PASSWORD -> {
                when {
                    Character.isWhitespace(codePoint) ->
                        reject("Password cannot contain spaces or line breaks.")
                    isRejectedControlCharacter(codePoint) ->
                        reject("Password contains unsupported characters.")
                    isEmojiCodePoint(codePoint) ->
                        reject("Emojis are not allowed in passwords.")
                    codePoint in 33..126 ->
                        builder.appendCodePoint(codePoint)
                    else -> reject("Password contains unsupported characters.")
                }
            }

            InputFieldPolicy.PHONE -> {
                when {
                    isRejectedControlCharacter(codePoint) ->
                        reject("Phone number cannot contain control characters.")
                    isEmojiCodePoint(codePoint) ->
                        reject("Emojis are not allowed.")
                    codePoint == '+'.code || codePoint == '-'.code || codePoint == '('.code ||
                        codePoint == ')'.code || Character.isDigit(codePoint) ||
                        codePoint == ' '.code -> builder.appendCodePoint(codePoint)
                    else -> reject("Phone number can only contain digits and + - ( )")
                }
            }

            InputFieldPolicy.DECIMAL -> {
                when {
                    Character.isWhitespace(codePoint) ->
                        reject("Spaces are not allowed in this field.")
                    isRejectedControlCharacter(codePoint) ->
                        reject("Only digits and one decimal point are allowed.")
                    isEmojiCodePoint(codePoint) ->
                        reject("Emojis are not allowed.")
                    Character.isDigit(codePoint) -> {
                        builder.appendCodePoint(codePoint)
                        if (maxNumericValue != null && builder.toString().toDoubleOrNull()?.let { it > maxNumericValue } == true) {
                            builder.deleteAt(builder.lastIndex)
                        }
                    }
                    codePoint == '.'.code && !seenDecimalSeparator -> {
                        seenDecimalSeparator = true
                        builder.append('.')
                        if (maxNumericValue != null && builder.toString().toDoubleOrNull()?.let { it > maxNumericValue } == true) {
                            builder.deleteAt(builder.lastIndex)
                            seenDecimalSeparator = false
                        }
                    }
                    codePoint == '.'.code ->
                        reject("Only one decimal point is allowed.")
                    else -> reject("Only digits and one decimal point are allowed.")
                }
            }
        }
    }

    var value = builder.toString()
    if (maxLength != null && maxLength > 0 && value.length > maxLength) {
        value = value.take(maxLength)
    }

    return SanitizedInput(value = value, rejectionMessage = rejectionMessage)
}

fun normalizeForCommit(value: String, fieldPolicy: InputFieldPolicy): String {
    return when (fieldPolicy) {
        InputFieldPolicy.GENERAL_TEXT,
        InputFieldPolicy.EMAIL,
        InputFieldPolicy.PASSWORD,
        InputFieldPolicy.PHONE,
        InputFieldPolicy.DECIMAL -> value.trim()
    }
}

fun validateCommittedInput(
    value: String,
    fieldPolicy: InputFieldPolicy,
    required: Boolean = false,
    maxLength: Int? = null,
    maxNumericValue: Double? = null,
    fieldName: String? = null
): String? {
    val normalizedValue = normalizeForCommit(value, fieldPolicy)

    if (required && normalizedValue.isEmpty()) {
        return "${fieldName ?: "This field"} cannot be blank."
    }

    if (normalizedValue.isEmpty()) {
        return null
    }

    if (maxLength != null && maxLength > 0 && normalizedValue.length > maxLength) {
        return "Maximum $maxLength characters."
    }

    val sanitized = sanitizeForEditing(
        raw = normalizedValue,
        fieldPolicy = fieldPolicy,
        maxLength = maxLength,
        maxNumericValue = maxNumericValue
    )
    if (sanitized.value != normalizedValue || sanitized.rejectionMessage != null) {
        return sanitized.rejectionMessage ?: "Invalid input."
    }

    return when (fieldPolicy) {
        InputFieldPolicy.EMAIL -> {
            val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
            if (!emailRegex.matches(normalizedValue)) "Invalid email format." else null
        }

        InputFieldPolicy.PHONE -> {
            if (!InputValidators.isValidFlexiblePhone(normalizedValue)) {
                "Invalid phone number format."
            } else null
        }

        InputFieldPolicy.DECIMAL -> {
            if (!decimalRegex.matches(normalizedValue)) {
                "Only digits and one decimal point are allowed."
            } else if (
                maxNumericValue != null &&
                normalizedValue.toDoubleOrNull()?.let { it > maxNumericValue } == true
            ) {
                "Value must be ${formatMaxNumericValue(maxNumericValue)} or less."
            } else null
        }

        InputFieldPolicy.GENERAL_TEXT,
        InputFieldPolicy.PASSWORD -> null
    }
}

fun validateLiveInput(
    value: String,
    fieldPolicy: InputFieldPolicy,
    maxNumericValue: Double? = null
): String? {
    if (containsOnlyWhitespace(value)) {
        return "Only spaces are not allowed."
    }

    return when (fieldPolicy) {
        InputFieldPolicy.DECIMAL -> {
            if (value.isBlank()) return null
            if (!value.matches(Regex("""\d*\.?\d*"""))) {
                "Only digits and one decimal point are allowed."
            } else if (
                maxNumericValue != null &&
                value.toDoubleOrNull()?.let { it > maxNumericValue } == true
            ) {
                "Value must be ${formatMaxNumericValue(maxNumericValue)} or less."
            } else null
        }

        else -> null
    }
}

fun containsOnlyWhitespace(value: String): Boolean {
    return value.isNotEmpty() && value.trim().isEmpty()
}

fun containsRejectedCharacters(value: String, fieldPolicy: InputFieldPolicy): Boolean {
    return sanitizeForEditing(value, fieldPolicy).value != value
}

fun String.trimToNullIfBlank(): String? {
    return trim().takeIf { it.isNotEmpty() }
}

private fun isRejectedControlCharacter(codePoint: Int): Boolean {
    return Character.isISOControl(codePoint) || codePoint == '\n'.code || codePoint == '\r'.code || codePoint == '\t'.code
}

private fun isEmojiCodePoint(codePoint: Int): Boolean {
    return codePoint > 0xFFFF ||
        codePoint == 0x200D ||
        codePoint == 0xFE0F ||
        codePoint in 0x2600..0x27BF
}

private fun formatMaxNumericValue(maxNumericValue: Double): String {
    return if (maxNumericValue % 1.0 == 0.0) {
        maxNumericValue.toInt().toString()
    } else {
        maxNumericValue.toString()
    }
}

private fun Int.asString(): String = String(Character.toChars(this))
