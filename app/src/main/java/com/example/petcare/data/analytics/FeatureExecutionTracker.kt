package com.example.petcare.data.analytics

import android.content.Context
import android.util.Log
import com.example.petcare.data.model.analytics.CreateFeatureExecutionLogRequest
import com.example.petcare.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Wraps API calls to measure execution time and network speed.
 *
 * Usage in ViewModels:
 * ```kotlin
 * val result = executionTracker.track(context, userId, featureName) {
 *     apiService.createPet(body)
 * }
 * ```
 */
object FeatureExecutionTracker {

    private const val TAG = "FeatureExecTracker"
    private val formatter = DateTimeFormatter.ISO_INSTANT

    /**
     * Execute [apiCall], measure its latency and network speed, then POST a log.
     *
     * @param context     Application context for network speed measurement.
     * @param userId      Current user's ID.
     * @param featureName Human-readable feature name (e.g. "Create Pet").
     *                    Must match a name in [AnalyticsSeedData.features].
     * @param apiCall     The suspend lambda that calls the API.
     * @return The [Response] from [apiCall] (passes through transparently).
     */
    suspend fun <T> track(
        context: Context,
        userId: String,
        featureName: String,
        apiCall: suspend () -> Response<T>
    ): Response<T> {
        val speed = NetworkSpeedMeasurer.measure(context)
        val startTime = Instant.now()

        // Execute the actual API call
        val response = apiCall()

        val endTime = Instant.now()
        val totalSeconds = java.time.Duration.between(startTime, endTime).seconds.toInt()

        // Fire-and-forget the log POST
        withContext(Dispatchers.IO) {
            try {
                val request = CreateFeatureExecutionLogRequest(
                    userId = userId,
                    featureId = featureName,  // backend resolves name to _id
                    startTime = formatter.format(startTime),
                    endTime = formatter.format(endTime),
                    totalTime = totalSeconds,
                    downloadSpeed = speed.downloadKbps,
                    uploadSpeed = speed.uploadKbps
                )
                val logResp = RetrofitClient.apiService.createFeatureExecutionLog(request)
                if (logResp.isSuccessful) {
                    Log.d(TAG, "Logged '$featureName' in ${totalSeconds}s (↓${speed.downloadKbps} ↑${speed.uploadKbps} kbps)")
                } else {
                    Log.w(TAG, "Failed to log '$featureName': ${logResp.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error logging feature execution: ${e.message}")
            }
        }

        return response
    }
}
