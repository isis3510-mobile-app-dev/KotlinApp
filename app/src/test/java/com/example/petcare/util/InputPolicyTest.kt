package com.example.petcare.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputPolicyTest {

    @Test
    fun `general text strips emoji and reports rejection`() {
        val sanitized = sanitizeForEditing(
            raw = "Buddy🐶",
            fieldPolicy = InputFieldPolicy.GENERAL_TEXT,
            maxLength = InputTextLimits.PET_NAME
        )

        assertEquals("Buddy", sanitized.value)
        assertEquals("Emojis are not allowed.", sanitized.rejectionMessage)
    }

    @Test
    fun `email removes spaces and rejects them`() {
        val sanitized = sanitizeForEditing(
            raw = "  buddy @mail.com  ",
            fieldPolicy = InputFieldPolicy.EMAIL,
            maxLength = InputTextLimits.EMAIL
        )

        assertEquals("buddy@mail.com", sanitized.value)
        assertEquals("Email cannot contain spaces.", sanitized.rejectionMessage)
    }

    @Test
    fun `password rejects whitespace and emoji`() {
        val sanitized = sanitizeForEditing(
            raw = "pa ss🐾",
            fieldPolicy = InputFieldPolicy.PASSWORD,
            maxLength = InputTextLimits.PASSWORD
        )

        assertEquals("pass", sanitized.value)
        assertTrue(sanitized.rejectionMessage != null)
    }

    @Test
    fun `containsOnlyWhitespace detects blank-but-not-empty strings`() {
        assertTrue(containsOnlyWhitespace("   "))
        assertFalse(containsOnlyWhitespace(" a "))
        assertFalse(containsOnlyWhitespace(""))
    }

    @Test
    fun `trimToNullIfBlank trims and nulls blank strings`() {
        assertEquals("Buddy", "  Buddy  ".trimToNullIfBlank())
        assertNull("   ".trimToNullIfBlank())
    }

    @Test
    fun `weight allows 199 point 0`() {
        val error = validateCommittedInput(
            value = "199.0",
            fieldPolicy = InputFieldPolicy.DECIMAL,
            maxLength = InputTextLimits.WEIGHT,
            maxNumericValue = 199.0,
            fieldName = "Weight"
        )

        assertNull(error)
    }

    @Test
    fun `weight rejects values above 199`() {
        val error = validateCommittedInput(
            value = "199.1",
            fieldPolicy = InputFieldPolicy.DECIMAL,
            maxLength = InputTextLimits.WEIGHT,
            maxNumericValue = 199.0,
            fieldName = "Weight"
        )

        assertEquals("Value must be 199 or less.", error)
    }

    @Test
    fun `weight rejects malformed decimals`() {
        val error = validateCommittedInput(
            value = "12..3",
            fieldPolicy = InputFieldPolicy.DECIMAL,
            maxLength = InputTextLimits.WEIGHT,
            maxNumericValue = 199.0,
            fieldName = "Weight"
        )

        assertEquals("Only one decimal point is allowed.", error)
    }
}
