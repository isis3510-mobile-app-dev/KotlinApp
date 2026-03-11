package com.example.petcare.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    // Offline Mode
    val offlineModeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[OFFLINE_MODE_ENABLED_KEY] ?: false
    }

    suspend fun setOfflineModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[OFFLINE_MODE_ENABLED_KEY] = enabled
        }
    }
}
