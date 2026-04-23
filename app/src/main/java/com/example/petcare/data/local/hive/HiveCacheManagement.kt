package com.example.petcare.data.local.hive


import android.content.Context
import androidx.core.content.edit

class HiveCacheManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "hive_api_cache", Context.MODE_PRIVATE
    )

    // ── TTL definidos por el equipo ───────────────────────────────────────

    companion object {
        private const val TTL_5_MIN  = 5 * 60 * 1000L       // 5 minutos en ms
        private const val TTL_12_HRS = 12 * 60 * 60 * 1000L // 12 horas en ms
    }

    // ── Escritura y lectura base ──────────────────────────────────────────

    private fun put(key: String, json: String) {
        prefs.edit {
            putString(key, json)
                .putLong("${key}_ts", System.currentTimeMillis())
        }
    }

    private fun get(key: String): String? =
        prefs.getString(key, null)

    private fun isFresh(key: String, ttlMs: Long): Boolean {
        val ts = prefs.getLong("${key}_ts", 0L)
        if (ts == 0L) return false
        return System.currentTimeMillis() - ts < ttlMs
    }

    private fun delete(key: String) {
        prefs.edit {
            remove(key)
                .remove("${key}_ts")
        }
    }

    // ── Pets (TTL: 5 minutos) ─────────────────────────────────────────────

    fun putPets(userId: String, json: String) =
        put("pets_$userId", json)

    fun getPets(userId: String): String? =
        get("pets_$userId").takeIf { isFresh("pets_$userId", TTL_5_MIN) }

    fun isPetsFresh(userId: String): Boolean =
        isFresh("pets_$userId", TTL_5_MIN)

    // ── Events por mascota (TTL: 5 minutos) ───────────────────────────────

    fun putEvents(petId: String, json: String) =
        put("events_$petId", json)

    fun getEvents(petId: String): String? =
        get("events_$petId").takeIf { isFresh("events_$petId", TTL_5_MIN) }

    fun isEventsFresh(petId: String): Boolean =
        isFresh("events_$petId", TTL_5_MIN)

    // ── Usuario actual (TTL: 5 minutos) ───────────────────────────────────

    fun putUser(userId: String, json: String) =
        put("user_$userId", json)

    fun getUser(userId: String): String? =
        get("user_$userId").takeIf { isFresh("user_$userId", TTL_5_MIN) }

    fun isUserFresh(userId: String): Boolean =
        isFresh("user_$userId", TTL_5_MIN)

    // ── Smart suggestions (TTL: 5 minutos) ───────────────────────────────

    fun putSuggestions(petId: String, json: String) =
        put("suggestions_$petId", json)

    fun getSuggestions(petId: String): String? =
        get("suggestions_$petId").takeIf { isFresh("suggestions_$petId", TTL_5_MIN) }

    fun isSuggestionsFresh(petId: String): Boolean =
        isFresh("suggestions_$petId", TTL_5_MIN)

    // ── Vacunas catálogo (TTL: 12 horas) ─────────────────────────────────

    fun putVaccineCatalog(json: String) =
        put("vaccine_catalog", json)

    fun getVaccineCatalog(): String? =
        get("vaccine_catalog").takeIf { isFresh("vaccine_catalog", TTL_12_HRS) }

    fun isVaccineCatalogFresh(): Boolean =
        isFresh("vaccine_catalog", TTL_12_HRS)

    // ── Vacuna por ID (TTL: 12 horas) ─────────────────────────────────────

    fun putVaccine(vaccineId: String, json: String) =
        put("vaccine_$vaccineId", json)

    fun getVaccine(vaccineId: String): String? =
        get("vaccine_$vaccineId").takeIf { isFresh("vaccine_$vaccineId", TTL_12_HRS) }

    fun isVaccineFresh(vaccineId: String): Boolean =
        isFresh("vaccine_$vaccineId", TTL_12_HRS)

    // ── Utilidad ──────────────────────────────────────────────────────────

    fun clearAll() = prefs.edit { clear() }

    fun invalidatePets(userId: String) = delete("pets_$userId")
    fun invalidateEvents(petId: String) = delete("events_$petId")
    fun invalidateSuggestions(petId: String) = delete("suggestions_$petId")
}