package com.example.petcare.data.analytics

import android.util.Log
import com.example.petcare.data.model.analytics.CreateFeatureClicksLogRequest
import com.example.petcare.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Tracks the number of taps along a FeatureRoute.
 *
 * Usage:
 * 1. `startRoute(routeId)` — called when the user taps the origin button.
 * 2. `recordClick()` — called on each intermediate tap.
 * 3. `endRoute(userId)` — called when the user taps the end button. POSTs the log.
 * 4. `cancelRoute()` — called when the user navigates away (back button, etc).
 *
 * Only one route can be active at a time.
 */
class FeatureClicksTracker(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val tag = "FeatureClicksTracker"
    private val formatter = DateTimeFormatter.ISO_INSTANT

    private var activeRouteId: String? = null
    private var clickCount: Int = 0

    /**
     * Begin tracking a new route. Cancels any previously active route.
     *
     * @param routeId The FeatureRoute name or ID to track.
     */
    fun startRoute(routeId: String) {
        if (activeRouteId != null) {
            Log.d(tag, "Replacing active route '$activeRouteId' with '$routeId'")
        }
        activeRouteId = routeId
        clickCount = 1  // The origin button tap counts as click #1
        Log.d(tag, "Started route '$routeId'")
    }

    /**
     * Record an intermediate tap along the active route.
     */
    fun recordClick() {
        if (activeRouteId == null) return
        clickCount++
    }

    /**
     * End the active route and POST the clicks log.
     *
     * @param userId The current user's ID.
     */
    fun endRoute(userId: String) {
        val routeId = activeRouteId ?: return

        val request = CreateFeatureClicksLogRequest(
            userId = userId,
            routeId = routeId,
            timestamp = formatter.format(Instant.now()),
            nClicks = clickCount
        )

        scope.launch {
            try {
                val resp = RetrofitClient.apiService.createFeatureClicksLog(request)
                if (resp.isSuccessful) {
                    Log.d(tag, "Logged $clickCount clicks for route '$routeId'")
                } else {
                    Log.w(tag, "Failed to log clicks for '$routeId': ${resp.code()}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error logging clicks: ${e.message}")
            }
        }

        activeRouteId = null
        clickCount = 0
    }

    /**
     * Cancel tracking the current route without logging.
     */
    fun cancelRoute() {
        if (activeRouteId != null) {
            Log.d(tag, "Cancelled route '$activeRouteId'")
        }
        activeRouteId = null
        clickCount = 0
    }

    /** Whether a route is currently being tracked. */
    val isTracking: Boolean get() = activeRouteId != null

    /** The currently tracked route ID, or null. */
    val currentRouteId: String? get() = activeRouteId
}
