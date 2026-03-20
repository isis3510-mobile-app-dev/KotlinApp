package com.example.petcare.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TextPolicyTest {

    @Test
    fun `truncateForDisplay keeps short text unchanged`() {
        assertEquals("Buddy", "Buddy".truncateForDisplay(14))
    }

    @Test
    fun `truncateForDisplay adds ellipsis when over limit`() {
        assertEquals("Golden Retr...", "Golden Retriever Puppy".truncateForDisplay(14))
    }

    @Test
    fun `enforceMaxLength hard caps input`() {
        assertEquals("12345", enforceMaxLength("123456789", 5))
    }
}
