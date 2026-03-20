package com.example.petcare.data.model

import com.google.gson.annotations.SerializedName

enum class ReminderWindow {
    WEEK,
    DAY,
    H2,
    H12
}

enum class VaccineUrgencyLevel {
    DANGER_ONLY,
    DANGER_AND_WARNING,
    MISSING_ONLY
}

data class AppNotification(
    val id: String,
    val schema: Int = 1,
    @SerializedName("userId") val userId: String,
    val type: String,
    val header: String,
    val text: String,
    @SerializedName("dateSent") val dateSent: String? = null,
    @SerializedName("dateClicked") val dateClicked: String? = null,
    @SerializedName("isRead") val isRead: Boolean = false,
    @SerializedName("isDismissed") val isDismissed: Boolean = false,
    @SerializedName("dateDismissed") val dateDismissed: String? = null
)

data class CreateNotificationRequest(
    @SerializedName("userId") val userId: String,
    val type: String,
    val header: String,
    val text: String,
    @SerializedName("isRead") val isRead: Boolean = false,
    @SerializedName("isDismissed") val isDismissed: Boolean = false
)

data class UpdateNotificationRequest(
    @SerializedName("dateClicked") val dateClicked: String? = null,
    @SerializedName("isRead") val isRead: Boolean? = null,
    @SerializedName("isDismissed") val isDismissed: Boolean? = null,
    @SerializedName("dateDismissed") val dateDismissed: String? = null
)
