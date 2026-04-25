package com.example.petcare.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.petcare.data.model.VaccineUrgencyLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property on Context for DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    // Keys
    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val OFFLINE_MODE_ENABLED_KEY = booleanPreferencesKey("offline_mode_enabled")
        val VACCINE_URGENCY_LEVEL_KEY = stringPreferencesKey("vaccine_urgency_level")
        val SENT_NOTIFICATION_KEYS = stringSetPreferencesKey("sent_notification_keys")
        val PREFERRED_WEIGHT_UNIT_KEY = stringPreferencesKey("preferred_weight_unit")
        val LAST_WEIGHT_TRACKER_PET_ID_KEY = stringPreferencesKey("last_weight_tracker_pet_id")
    }

    // Theme Mode
    val themeMode: Flow<AppThemeMode> = dataStore.data.map { preferences ->
        val themeString = preferences[THEME_MODE_KEY] ?: AppThemeMode.SYSTEM.name
        try {
            AppThemeMode.valueOf(themeString)
        } catch (e: IllegalArgumentException) {
            AppThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    // Notifications
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: false
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    val vaccineUrgencyLevel: Flow<VaccineUrgencyLevel> = dataStore.data.map { preferences ->
        val raw = preferences[VACCINE_URGENCY_LEVEL_KEY] ?: VaccineUrgencyLevel.DANGER_ONLY.name
        runCatching { VaccineUrgencyLevel.valueOf(raw) }.getOrDefault(VaccineUrgencyLevel.DANGER_ONLY)
    }

    suspend fun setVaccineUrgencyLevel(level: VaccineUrgencyLevel) {
        dataStore.edit { preferences ->
            preferences[VACCINE_URGENCY_LEVEL_KEY] = level.name
        }
    }

    val sentNotificationKeys: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[SENT_NOTIFICATION_KEYS] ?: emptySet()
    }

    suspend fun addSentNotificationKey(key: String) {
        dataStore.edit { preferences ->
            val current = preferences[SENT_NOTIFICATION_KEYS] ?: emptySet()
            preferences[SENT_NOTIFICATION_KEYS] = current + key
        }
    }

    // Offline Mode
    val offlineModeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[OFFLINE_MODE_ENABLED_KEY] ?: false
    }

    suspend fun setOfflineModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[OFFLINE_MODE_ENABLED_KEY] = enabled
        }
    }

    val preferredWeightUnit: Flow<String> = dataStore.data.map { preferences ->
        preferences[PREFERRED_WEIGHT_UNIT_KEY] ?: "kg"
    }

    suspend fun setPreferredWeightUnit(unit: String) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_WEIGHT_UNIT_KEY] = unit
        }
    }

    val lastWeightTrackerPetId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[LAST_WEIGHT_TRACKER_PET_ID_KEY]
    }

    suspend fun setLastWeightTrackerPetId(petId: String) {
        dataStore.edit { preferences ->
            preferences[LAST_WEIGHT_TRACKER_PET_ID_KEY] = petId
        }
    }
}
