package com.example.petcare.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.petcare.data.model.ReminderWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Member B — DataStore para preferencias de notificaciones por mascota.
 *
 * Guarda:
 *   - Toggle de notificaciones por mascota
 *   - Ventana de recordatorio por mascota (WEEK, DAY, H2, H12)
 *
 * Ventanas disponibles definidas por el equipo:
 *   WEEK  → 7 días antes
 *   DAY   → 1 día antes
 *   H12   → 12 horas antes
 *   H2    → 2 horas antes
 *
 * Flow-native, sin ANR risk, sin migraciones manuales.
 */
class NotificationPreferencesDataStore(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        // Ventana por defecto si el usuario no ha configurado nada
        private val DEFAULT_WINDOW = ReminderWindow.DAY.name
    }

    // ── Toggle de notificaciones por mascota ──────────────────────────────

    fun isNotifEnabled(petId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("notif_$petId")
        return dataStore.data.map { it[key] ?: true }
    }

    suspend fun setNotifEnabled(petId: String, enabled: Boolean) {
        val key = booleanPreferencesKey("notif_$petId")
        dataStore.edit { prefs -> prefs[key] = enabled }
    }

    // ── Ventana de recordatorio por mascota ───────────────────────────────

    fun getReminderWindow(petId: String): Flow<ReminderWindow> {
        val key = stringPreferencesKey("window_$petId")
        return dataStore.data.map { prefs ->
            val raw = prefs[key] ?: DEFAULT_WINDOW
            runCatching { ReminderWindow.valueOf(raw) }
                .getOrDefault(ReminderWindow.DAY)
        }
    }

    suspend fun setReminderWindow(petId: String, window: ReminderWindow) {
        val key = stringPreferencesKey("window_$petId")
        dataStore.edit { prefs -> prefs[key] = window.name }
    }

    // ── Convierte ReminderWindow a ms para calcular triggerMs ─────────────

    fun windowToMs(window: ReminderWindow): Long = when (window) {
        ReminderWindow.WEEK -> 7L * 24 * 60 * 60 * 1000
        ReminderWindow.DAY  -> 1L * 24 * 60 * 60 * 1000
        ReminderWindow.H12  -> 12L * 60 * 60 * 1000
        ReminderWindow.H2   -> 2L  * 60 * 60 * 1000
    }

    // ── Utilidad: calcular cuándo disparar el reminder ────────────────────

    /**
     * Dado el timestamp del evento y la ventana preferida,
     * retorna el epoch ms en que debe dispararse la notificación.
     *
     * Ejemplo: evento a las 3pm, ventana DAY → trigger a las 3pm del día anterior
     */
    fun calculateTriggerMs(eventTimeMs: Long, window: ReminderWindow): Long =
        eventTimeMs - windowToMs(window)
}