package com.example.petcare.data.analytics

import android.content.Context
import android.util.Log
import com.example.petcare.data.model.analytics.CreateFeatureExecutionLogRequest
import com.example.petcare.data.repository.RepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Wraps repository calls to measure execution time and network speed.
 *
 * Usage in ViewModels:
 * ```kotlin
 * val result = FeatureExecutionTracker.track("Create Pet") {
 *     petRepository.createPet(request)
 * }
 * result.fold(onSuccess = { ... }, onFailure = { ... })
 * ```
 *
 * Call [init] once from [com.example.petcare.PetCareApplication.onCreate].
 */
object FeatureExecutionTracker {

    private const val TAG = "FeatureExecTracker"
    private val formatter = DateTimeFormatter.ISO_INSTANT
    private val logScope = CoroutineScope(Dispatchers.IO)

    private var appContext: Context? = null

    /** Call once from Application.onCreate() */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Execute [block], measure its latency and network speed, then POST a log.
     *
     * @param featureName Human-readable feature name (e.g. "Create Pet").
     * @param block       The suspend lambda (typically a repository call) to measure.
     * @return The [Result] from [block] — passes through transparently.
     */
    suspend fun <T> track(
        featureName: String,
        block: suspend () -> Result<T>
    ): Result<T> {
        val ctx = appContext
        val speed = if (ctx != null) NetworkSpeedMeasurer.measure(ctx)
                    else NetworkSpeedMeasurer.Speed(0, 0)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
        val startTime = Instant.now()

        val result = block()

        val endTime = Instant.now()
        val totalMillis = java.time.Duration.between(startTime, endTime).toMillis().toInt()

        // Fire-and-forget the analytics log
        logScope.launch {
            try {
                val request = CreateFeatureExecutionLogRequest(
                    userId = userId,
                    featureId = featureName,
                    startTime = formatter.format(startTime),
                    endTime = formatter.format(endTime),
                    totalTime = totalMillis,
                    downloadSpeed = speed.downloadKbps,
                    uploadSpeed = speed.uploadKbps
                )
                val logResp = RepositoryProvider.apiService.createFeatureExecutionLog(request)
                if (logResp.isSuccessful) {
                    Log.d(TAG, "Logged '$featureName' in ${totalMillis}ms (↓${speed.downloadKbps} ↑${speed.uploadKbps} kbps)")
                } else {
                    Log.w(TAG, "Failed to log '$featureName': ${logResp.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error logging feature execution: ${e.message}")
            }
        }

        return result
    }
}
