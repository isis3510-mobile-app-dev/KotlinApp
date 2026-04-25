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
            putLong("${key}_ts", System.currentTimeMillis())
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
            remove("${key}_ts")
        }
    }

    // ── Pets (TTL: 5 minutos) ─────────────────────────────────────────────

    fun putPets(userId: String, json: String) =
        put("pets_$userId", json)

    fun getPets(userId: String): String? =
        get("pets_$userId").takeIf { isFresh("pets_$userId", TTL_5_MIN) }

    fun isPetsFresh(userId: String): Boolean =
        isFresh("pets_$userId", TTL_5_MIN)

    fun invalidatePets(userId: String) = delete("pets_$userId")

    // ── Events por mascota (TTL: 5 minutos) ───────────────────────────────

    fun putEvents(petId: String, json: String) =
        put("events_$petId", json)

    fun getEvents(petId: String): String? =
        get("events_$petId").takeIf { isFresh("events_$petId", TTL_5_MIN) }

    fun isEventsFresh(petId: String): Boolean =
        isFresh("events_$petId", TTL_5_MIN)

    fun invalidateEvents(petId: String) = delete("events_$petId")

    // ── Events por owner (TTL: 5 minutos) ────────────────────────────────
    // Usado cuando se cargan eventos del dueño en Home/Records sin petId específico

    fun putEventsByOwner(ownerId: String, json: String) =
        put("events_owner_$ownerId", json)

    fun getEventsByOwner(ownerId: String): String? =
        get("events_owner_$ownerId").takeIf { isFresh("events_owner_$ownerId", TTL_5_MIN) }

    fun isEventsByOwnerFresh(ownerId: String): Boolean =
        isFresh("events_owner_$ownerId", TTL_5_MIN)

    fun invalidateEventsByOwner(ownerId: String) = delete("events_owner_$ownerId")

    // ── Usuario actual (TTL: 5 minutos) ───────────────────────────────────

    fun putUser(userId: String, json: String) =
        put("user_$userId", json)

    fun getUser(userId: String): String? =
        get("user_$userId").takeIf { isFresh("user_$userId", TTL_5_MIN) }

    fun isUserFresh(userId: String): Boolean =
        isFresh("user_$userId", TTL_5_MIN)

    fun invalidateUser(userId: String) = delete("user_$userId")

    // ── Smart suggestions (TTL: 5 minutos) ───────────────────────────────

    fun putSuggestions(petId: String, json: String) =
        put("suggestions_$petId", json)

    fun getSuggestions(petId: String): String? =
        get("suggestions_$petId").takeIf { isFresh("suggestions_$petId", TTL_5_MIN) }

    fun isSuggestionsFresh(petId: String): Boolean =
        isFresh("suggestions_$petId", TTL_5_MIN)

    fun invalidateSuggestions(petId: String) = delete("suggestions_$petId")

    // ── Pending vaccination documents (no TTL: offline queue) ───────────

    fun putPendingVaccinationDocuments(
        petId: String,
        vaccinationId: String,
        json: String
    ) = put("pending_vax_docs_${petId}_$vaccinationId", json)

    fun getPendingVaccinationDocuments(
        petId: String,
        vaccinationId: String
    ): String? = get("pending_vax_docs_${petId}_$vaccinationId")

    fun invalidatePendingVaccinationDocuments(
        petId: String,
        vaccinationId: String
    ) = delete("pending_vax_docs_${petId}_$vaccinationId")

    fun getAllPendingVaccinationDocumentJson(): Map<String, String> =
        prefs.all
            .filterKeys { it.startsWith("pending_vax_docs_") && !it.endsWith("_ts") }
            .mapValues { it.value as? String ?: "" }
            .filterValues { it.isNotBlank() }

    fun putPendingVaccinationDocumentsByKey(key: String, json: String) =
        put(key, json)

    fun invalidatePendingVaccinationDocumentsByKey(key: String) = delete(key)

    fun movePendingVaccinationDocuments(
        oldPetId: String,
        oldVaccinationId: String,
        newPetId: String,
        newVaccinationId: String,
        transformJson: (String) -> String
    ) {
        val oldKey = "pending_vax_docs_${oldPetId}_$oldVaccinationId"
        val json = get(oldKey) ?: return
        put("pending_vax_docs_${newPetId}_$newVaccinationId", transformJson(json))
        delete(oldKey)
    }

    // ── Pending event documents (no TTL: offline queue) ─────────────────

    fun putPendingEventDocuments(
        petId: String,
        eventId: String,
        json: String
    ) = put("pending_event_docs_${petId}_$eventId", json)

    fun getPendingEventDocuments(
        petId: String,
        eventId: String
    ): String? = get("pending_event_docs_${petId}_$eventId")

    fun invalidatePendingEventDocuments(
        petId: String,
        eventId: String
    ) = delete("pending_event_docs_${petId}_$eventId")

    fun getAllPendingEventDocumentJson(): Map<String, String> =
        prefs.all
            .filterKeys { it.startsWith("pending_event_docs_") && !it.endsWith("_ts") }
            .mapValues { it.value as? String ?: "" }
            .filterValues { it.isNotBlank() }

    fun putPendingEventDocumentsByKey(key: String, json: String) =
        put(key, json)

    fun invalidatePendingEventDocumentsByKey(key: String) = delete(key)

    fun movePendingEventDocuments(
        oldPetId: String,
        oldEventId: String,
        newPetId: String,
        newEventId: String,
        transformJson: (String) -> String
    ) {
        val oldKey = "pending_event_docs_${oldPetId}_$oldEventId"
        val json = get(oldKey) ?: return
        put("pending_event_docs_${newPetId}_$newEventId", transformJson(json))
        delete(oldKey)
    }

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

    // Invalida todo lo relacionado a un pet de una vez
    fun invalidateAllForPet(petId: String, userId: String) {
        invalidatePets(userId)
        invalidateEvents(petId)
        invalidateSuggestions(petId)
    }

    // Solo para debug/testing — pone todos los timestamps en 0
    // para forzar que isFresh() devuelva false en el próximo acceso
    fun expireAllForTesting() {
        prefs.edit {
            prefs.all.keys
                .filter { it.endsWith("_ts") }
                .forEach { key -> putLong(key, 0L) }
        }
    }
}
