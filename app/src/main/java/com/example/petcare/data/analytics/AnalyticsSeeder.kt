package com.example.petcare.data.analytics

import android.content.Context
import android.util.Log
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds analytics metadata (screens, features, routes) to the backend.
 */
object AnalyticsSeeder {

    private const val TAG = "AnalyticsSeeder"
    private const val PREFS_NAME = "analytics_prefs"
    private const val KEY_SEEDED = "analytics_seeded"

    /**
     * Call from Application.onCreate() or the first Activity.
     * Runs on [Dispatchers.IO].
     *
     * Always verifies backend completeness for Kotlin metadata and only sends
     * missing records. This keeps seeding self-healing after partial wipes.
     */
    suspend fun seedIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hadSeedFlag = prefs.getBoolean(KEY_SEEDED, false)

        try {
            val api = RepositoryProvider.apiService

            // Always verify completeness against backend state.
            val respScreens = api.getScreens(appType = "Kotlin")
            val respFeatures = api.getFeatures(appType = "Kotlin")
            val respRoutes = api.getFeatureRoutes(appType = "Kotlin")

            if (!respScreens.isSuccessful || !respFeatures.isSuccessful || !respRoutes.isSuccessful) {
                Log.w(
                    TAG,
                    "Cannot verify metadata completeness " +
                        "(screens=${respScreens.code()}, features=${respFeatures.code()}, routes=${respRoutes.code()})."
                )
                return@withContext
            }

            val existingScreens = respScreens.body().orEmpty().map { it.name }.toSet()
            val existingFeatures = respFeatures.body().orEmpty().map { it.name }.toSet()
            val existingRoutes = respRoutes.body().orEmpty().map { it.name }.toSet()

            val missingScreens = AnalyticsSeedData.screens.filter { it.name !in existingScreens }
            val missingFeatures = AnalyticsSeedData.features.filter { it.name !in existingFeatures }
            val missingRoutes = AnalyticsSeedData.featureRoutes.filter { it.name !in existingRoutes }

            if (missingScreens.isEmpty() && missingFeatures.isEmpty() && missingRoutes.isEmpty()) {
                if (!hadSeedFlag) {
                    prefs.edit().putBoolean(KEY_SEEDED, true).apply()
                }
                Log.d(TAG, "Kotlin analytics metadata already complete.")
                return@withContext
            }

            Log.d(
                TAG,
                "Missing metadata → screens=${missingScreens.size}, " +
                    "features=${missingFeatures.size}, routes=${missingRoutes.size}. Seeding missing records…"
            )

            var hadErrors = false

            for (screen in missingScreens) {
                val resp = api.createScreen(screen)
                if (!resp.isSuccessful) {
                    hadErrors = true
                    Log.w(TAG, "Failed to seed screen '${screen.name}': ${resp.code()}")
                }
            }

            for (feature in missingFeatures) {
                val resp = api.createFeature(feature)
                if (!resp.isSuccessful) {
                    hadErrors = true
                    Log.w(TAG, "Failed to seed feature '${feature.name}': ${resp.code()}")
                }
            }

            for (route in missingRoutes) {
                val resp = api.createFeatureRoute(route)
                if (!resp.isSuccessful) {
                    hadErrors = true
                    Log.w(TAG, "Failed to seed route '${route.name}': ${resp.code()}")
                }
            }

            if (!hadErrors) {
                prefs.edit().putBoolean(KEY_SEEDED, true).apply()
                Log.d(TAG, "Seeding complete.")
            } else {
                Log.w(TAG, "Seeding completed with errors; will retry next launch.")
            }

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
