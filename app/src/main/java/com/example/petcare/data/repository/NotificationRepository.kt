package com.example.petcare.data.repository

import com.example.petcare.data.model.AppNotification
import com.example.petcare.data.model.CreateNotificationRequest
import com.example.petcare.data.model.UpdateNotificationRequest
import com.example.petcare.data.network.ApiService
import org.json.JSONObject

class NotificationRepository(private val api: ApiService) {

    suspend fun createNotification(request: CreateNotificationRequest): Result<AppNotification> = runCatching {
        val response = api.createNotification(request)
        if (!response.isSuccessful) {
            error(parseApiError(response.errorBody()?.string(), response.code(), "create notification"))
        }
        response.body() ?: error("Failed to create notification — empty response")
    }

    suspend fun markNotificationClicked(notificationId: String, clickedAtIso: String): Result<AppNotification> =
        runCatching {
            val response = api.updateNotification(
                notificationId = notificationId,
                body = UpdateNotificationRequest(
                    isRead = true,
                    dateClicked = clickedAtIso
                )
            )
            if (!response.isSuccessful) {
                error(parseApiError(response.errorBody()?.string(), response.code(), "mark notification clicked"))
            }
            response.body() ?: error("Failed to mark notification clicked — empty response")
        }

    suspend fun markNotificationDismissed(notificationId: String, dismissedAtIso: String): Result<AppNotification> =
        runCatching {
            val response = api.updateNotification(
                notificationId = notificationId,
                body = UpdateNotificationRequest(
                    isDismissed = true,
                    dateDismissed = dismissedAtIso
                )
            )
            if (!response.isSuccessful) {
                error(parseApiError(response.errorBody()?.string(), response.code(), "mark notification dismissed"))
            }
            response.body() ?: error("Failed to mark notification dismissed — empty response")
        }

    private fun parseApiError(errorBody: String?, code: Int, action: String): String {
        if (errorBody.isNullOrBlank()) {
            return "Failed to $action — HTTP $code"
        }

        return runCatching {
            val json = JSONObject(errorBody)
            json.optString("error")
                .ifBlank { json.optString("message") }
                .ifBlank { errorBody }
        }.getOrDefault(errorBody).let { message ->
            "Failed to $action — HTTP $code: $message"
        }
    }
}
