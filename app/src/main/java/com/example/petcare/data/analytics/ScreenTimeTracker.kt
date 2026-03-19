package com.example.petcare.data.analytics

import android.util.Log
import androidx.navigation.NavController
import com.example.petcare.data.model.analytics.CreateScreenTimeLogRequest
import com.example.petcare.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Tracks time spent on each screen by listening to NavController destination changes.
 *
 * - Records `startTime` when entering a screen.
 * - When the user navigates away, computes `totalTime` and POSTs a log.
 * - Uses a background coroutine scope for non-blocking API calls.
 */
class ScreenTimeTracker(
    private val userId: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val tag = "ScreenTimeTracker"

    private var currentScreen: String? = null
    private var enterTime: Instant? = null

    /**
     * Attach to a NavController to automatically track screen transitions.
     */
    fun attach(navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val newRoute = destination.route?.substringBefore("/")  // strip parameters
                ?: return@addOnDestinationChangedListener

            // Log time for the previous screen
            logCurrentScreen()

            // Start tracking the new screen
            currentScreen = newRoute
            enterTime = Instant.now()
        }
    }

    /**
     * Call when the app goes to background or user logs out to flush the current screen.
     */
    fun flush() {
        logCurrentScreen()
        currentScreen = null
        enterTime = null
    }

    private fun logCurrentScreen() {
        val screen = currentScreen ?: return
        val start = enterTime ?: return

        val end = Instant.now()
        val totalSeconds = java.time.Duration.between(start, end).seconds.toInt()

        // Skip very short visits (< 1s) — likely just navigation transitions
        if (totalSeconds < 1) return

        val formatter = DateTimeFormatter.ISO_INSTANT
        val request = CreateScreenTimeLogRequest(
            userId = userId,
            screenId = screen,   // Uses screen name; backend resolves to _id
            startTime = formatter.format(start),
            endTime = formatter.format(end),
            totalTime = totalSeconds
        )

        scope.launch {
            try {
                val resp = RetrofitClient.apiService.createScreenTimeLog(request)
                if (resp.isSuccessful) {
                    Log.d(tag, "Logged $totalSeconds s on '$screen'")
                } else {
                    Log.w(tag, "Failed to log screen time for '$screen': ${resp.code()}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error logging screen time: ${e.message}")
            }
        }
    }
}
