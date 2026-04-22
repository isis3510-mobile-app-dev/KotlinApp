package com.example.petcare.util

object DisplayTextLimits {
    const val HOME_PET_CARD = 14
    const val COMPACT_TITLE = 28
    const val SUBTITLE_META = 36
    const val LONG_SNIPPET = 120
    const val DETAIL_TITLE = 60
    const val DETAIL_BODY = 240
}

object InputTextLimits {
    const val PET_NAME = 30
    const val BREED = 40
    const val COLOR = 30
    const val EVENT_TITLE = 60
    const val PROVIDER_OR_CLINIC = 60
    const val NOTES = 200
    const val USER_NAME = 60
    const val PHONE = 20
    const val ADDRESS = 120
    const val LOT_NUMBER = 30
    const val EMAIL = 254
    const val PASSWORD = 64
    const val WEIGHT = 5
    const val PRICE = 10
}

fun String.truncateForDisplay(limit: Int): String {
    if (limit <= 0) return ""
    if (length <= limit) return this
    if (limit <= 3) return ".".repeat(limit)
    return take(limit - 3).trimEnd() + "..."
}

fun enforceMaxLength(value: String, maxLength: Int?): String {
    if (maxLength == null || maxLength <= 0) return value
    return value.take(maxLength)
}
