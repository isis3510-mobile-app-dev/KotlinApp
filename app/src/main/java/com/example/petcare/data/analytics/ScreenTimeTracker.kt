package com.example.petcare.data.analytics

import android.util.Log
import androidx.navigation.NavController
import com.example.petcare.data.model.analytics.CreateScreenTimeLogRequest
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
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
    private var attachedNavController: NavController? = null

    private val destinationListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        val rawRoute = destination.route ?: return@OnDestinationChangedListener
        val newRoute = normalizeRoute(rawRoute)

        // Log time for the previous screen
        logCurrentScreen()

        // Start tracking the new screen
        currentScreen = newRoute
        enterTime = Instant.now()
    }

    /**
     * Attach to a NavController to automatically track screen transitions.
     */
    fun attach(navController: NavController) {
        if (attachedNavController === navController) return
        attachedNavController?.removeOnDestinationChangedListener(destinationListener)
        attachedNavController = navController
        navController.addOnDestinationChangedListener(destinationListener)
    }

    /**
     * Detach the current destination listener from the NavController.
     */
    fun detach(navController: NavController? = attachedNavController) {
        val target = navController ?: return
        target.removeOnDestinationChangedListener(destinationListener)
        if (attachedNavController === target) {
            attachedNavController = null
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

    private fun normalizeRoute(route: String): String {
        val argsStart = route.indexOf("/{")
        return if (argsStart >= 0) route.substring(0, argsStart) else route
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
                val resp = RepositoryProvider.apiService.createScreenTimeLog(request)
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
