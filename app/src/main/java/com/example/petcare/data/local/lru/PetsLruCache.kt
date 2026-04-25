package com.example.petcare.data.local.lru

import android.util.LruCache
import com.example.petcare.data.model.Pet
import com.example.petcare.data.model.Vaccination


class PetLruCache {

    // ── Configuración ────────────────────────────────────────────────────────

    companion object {
        private const val PET_MAX_ENTRIES       = 15
        private const val VACCINE_MAX_ENTRIES   = 120
        private const val PET_TTL_MS            = 5 * 60 * 1_000L   // 5 min
        private const val VACCINE_TTL_MS        = 2 * 60 * 1_000L   // 2 min
    }

    // ── Estructuras internas ──────────────────────────────────────────────────

    private data class CacheEntry<T>(
        val value: T,
        val expiresAt: Long          // System.currentTimeMillis() + TTL
    ) {
        val isExpired get() = System.currentTimeMillis() > expiresAt
    }

    private val petCache = object : LruCache<String, CacheEntry<List<Pet>>>(PET_MAX_ENTRIES) {
        override fun sizeOf(key: String, value: CacheEntry<List<Pet>>) = 1
    }

    private val vaccineCache = object : LruCache<String, CacheEntry<List<Vaccination>>>(VACCINE_MAX_ENTRIES) {
        override fun sizeOf(key: String, value: CacheEntry<List<Vaccination>>) = 1
    }

    // ── Pets ──────────────────────────────────────────────────────────────────

    fun getPets(uid: String): List<Pet>? {
        val entry = petCache.get("pets:$uid") ?: return null
        if (entry.isExpired) {
            petCache.remove("pets:$uid")
            return null
        }
        return entry.value
    }

    fun putPets(uid: String, pets: List<Pet>) {
        petCache.put(
            "pets:$uid",
            CacheEntry(pets, System.currentTimeMillis() + PET_TTL_MS)
        )
    }

    fun getPet(uid: String, petId: String): Pet? {
        val entry = petCache.get("pet:$uid:$petId") ?: return null
        if (entry.isExpired) {
            petCache.remove("pet:$uid:$petId")
            return null
        }
        return entry.value.firstOrNull()
    }

    fun putPet(uid: String, pet: Pet) {
        petCache.put(
            "pet:$uid:${pet.id}",
            CacheEntry(listOf(pet), System.currentTimeMillis() + PET_TTL_MS)
        )
        // Invalida la lista completa para que la próxima llamada a getPets
        // no devuelva datos inconsistentes con el pet actualizado.
        petCache.remove("pets:$uid")
    }

    fun invalidatePet(uid: String, petId: String) {
        petCache.remove("pet:$uid:$petId")
        petCache.remove("pets:$uid")
    }

    fun invalidateAllPets(uid: String) {
        // LruCache no tiene "removeIf" nativo; iteramos el snapshot
        petCache.snapshot().keys
            .filter { it.startsWith("pets:$uid") || it.startsWith("pet:$uid:") }
            .forEach { petCache.remove(it) }
    }

    // ── Vacunas ───────────────────────────────────────────────────────────────

    /**
     * Clave: "vax:$petId:rev=$revision"
     * La revisión (catalogRevision) se incrementa cuando cambia el catálogo
     * de vacunas, invalidando automáticamente entradas antiguas sin necesidad
     * de recorrer el caché.
     */
    fun getVaccinations(petId: String, revision: Int): List<Vaccination>? {
        val key = vaccineKey(petId, revision)
        val entry = vaccineCache.get(key) ?: return null
        if (entry.isExpired) {
            vaccineCache.remove(key)
            return null
        }
        return entry.value
    }

    fun putVaccinations(petId: String, revision: Int, vaccinations: List<Vaccination>) {
        vaccineCache.put(
            vaccineKey(petId, revision),
            CacheEntry(vaccinations, System.currentTimeMillis() + VACCINE_TTL_MS)
        )
    }

    fun invalidateVaccinations(petId: String) {
        vaccineCache.snapshot().keys
            .filter { it.startsWith("vax:$petId:") }
            .forEach { vaccineCache.remove(it) }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    fun invalidateAll(uid: String) {
        invalidateAllPets(uid)
        // Para vacunas no tenemos uid en la clave, pero sí podemos limpiar todo
        // si el usuario cierra sesión.
        vaccineCache.evictAll()
    }

    /** Métricas útiles para debugging / logging */
    fun stats(): String {
        return "PetCache: ${petCache.size()}/${PET_MAX_ENTRIES} entries, " +
                "hitCount=${petCache.hitCount()}, missCount=${petCache.missCount()} | " +
                "VaxCache: ${vaccineCache.size()}/${VACCINE_MAX_ENTRIES} entries, " +
                "hitCount=${vaccineCache.hitCount()}, missCount=${vaccineCache.missCount()}"
    }

    private fun vaccineKey(petId: String, revision: Int) = "vax:$petId:rev=$revision"
}