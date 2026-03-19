package com.example.petcare.data.analytics

import android.content.Context
import android.util.Log
import com.example.petcare.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds analytics metadata (screens, features, routes) to the backend.
 *
 * - First launch on this device → checks backend for existing Kotlin data.
 *   If found, just marks as seeded. If empty, POSTs all seed data.
 * - Subsequent launches → skips entirely (SharedPreferences flag).
 */
object AnalyticsSeeder {

    private const val TAG = "AnalyticsSeeder"
    private const val PREFS_NAME = "analytics_prefs"
    private const val KEY_SEEDED = "analytics_seeded"

    /**
     * Call from Application.onCreate() or the first Activity.
     * Runs on [Dispatchers.IO] — does nothing if already seeded.
     */
    suspend fun seedIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (prefs.getBoolean(KEY_SEEDED, false)) {
            Log.d(TAG, "Already seeded on this device — skipping.")
            return@withContext
        }

        try {
            val api = RetrofitClient.apiService

            // Check if Kotlin screens already exist on the backend
            val existingScreens = api.getScreens(appType = "Kotlin")
            if (existingScreens.isSuccessful && existingScreens.body().orEmpty().isNotEmpty()) {
                Log.d(TAG, "Backend already has Kotlin screens — marking seeded.")
                prefs.edit().putBoolean(KEY_SEEDED, true).apply()
                return@withContext
            }

            // ─── Seed Screens ───────────────────────────────────────────
            Log.d(TAG, "Seeding ${AnalyticsSeedData.screens.size} screens…")
            for (screen in AnalyticsSeedData.screens) {
                val resp = api.createScreen(screen)
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Failed to seed screen '${screen.name}': ${resp.code()}")
                }
            }

            // ─── Seed Features ──────────────────────────────────────────
            Log.d(TAG, "Seeding ${AnalyticsSeedData.features.size} features…")
            for (feature in AnalyticsSeedData.features) {
                val resp = api.createFeature(feature)
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Failed to seed feature '${feature.name}': ${resp.code()}")
                }
            }

            // ─── Seed Feature Routes ────────────────────────────────────
            Log.d(TAG, "Seeding ${AnalyticsSeedData.featureRoutes.size} routes…")
            for (route in AnalyticsSeedData.featureRoutes) {
                val resp = api.createFeatureRoute(route)
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Failed to seed route '${route.name}': ${resp.code()}")
                }
            }

            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
            Log.d(TAG, "Seeding complete.")

        } catch (e: Exception) {
            Log.e(TAG, "Seeding failed (will retry on next launch): ${e.message}", e)
            // Don't mark as seeded — will retry on next launch
        }
    }

    /**
     * Force re-seed on next launch (e.g. after a wipe in analytics_manager.py).
     */
    fun resetSeedFlag(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_SEEDED).apply()
    }
}
