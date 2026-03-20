package com.example.petcare.util

object InputValidators {
    private val phoneRegex = Regex("^\\+?[0-9\\s\\-()]{7,20}$")

    fun isValidFlexiblePhone(value: String): Boolean {
        return phoneRegex.matches(value)
    }
}
