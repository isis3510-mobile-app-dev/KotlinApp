package com.example.petcare.data.analytics

import android.util.Log
import com.example.petcare.data.model.analytics.CreateFeatureClicksLogRequest
import com.example.petcare.data.repository.RepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Tracks the number of taps along a FeatureRoute.
 *
 * Usage in Composables:
 * 1. `FeatureClicksTracker.startRoute("Add Pet Flow")` — in the origin button handler.
 * 2. `FeatureClicksTracker.recordClick()` — in intermediate button handlers.
 * 3. `FeatureClicksTracker.endRoute()` — in the final "submit/create" button handler.
 * 4. `FeatureClicksTracker.cancelRoute()` — if the user leaves the flow.
 */
object FeatureClicksTracker {

    private const val TAG = "FeatureClicksTracker"
    private val formatter = DateTimeFormatter.ISO_INSTANT
    private val scope = CoroutineScope(Dispatchers.IO)

    private var activeRouteId: String? = null
    private var clickCount: Int = 0

    /** Start tracking a new route. */
    fun startRoute(routeId: String) {
        if (activeRouteId != null) {
            Log.d(TAG, "Replacing active route '$activeRouteId' with '$routeId'")
        }
        activeRouteId = routeId
        clickCount = 1 // Count the first click
        Log.d(TAG, "Started route '$routeId'")
    }

    /** Increment click count on the active route. */
    fun recordClick() {
        if (activeRouteId == null) return
        clickCount++
    }

    /** End the route and log results to the backend. */
    fun endRoute() {
        val routeId = activeRouteId ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

        val request = CreateFeatureClicksLogRequest(
            userId = userId,
            routeId = routeId,
            timestamp = formatter.format(Instant.now()),
            nClicks = clickCount
        )

        scope.launch {
            try {
                val resp = RepositoryProvider.apiService.createFeatureClicksLog(request)
                if (resp.isSuccessful) {
                    Log.d(TAG, "Logged $clickCount clicks for route '$routeId'")
                } else {
                    Log.w(TAG, "Failed to log clicks for '$routeId': ${resp.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error logging feature clicks: ${e.message}")
            }
        }

        activeRouteId = null
        clickCount = 0
    }

    /** Cancel the current route without logging. */
    fun cancelRoute() {
        if (activeRouteId != null) {
            Log.d(TAG, "Cancelled route '$activeRouteId'")
        }
        activeRouteId = null
        clickCount = 0
    }

    val isTracking: Boolean get() = activeRouteId != null
}
