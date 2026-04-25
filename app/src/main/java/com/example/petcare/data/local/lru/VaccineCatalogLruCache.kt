package com.example.petcare.data.local.lru

import com.example.petcare.data.model.Vaccine

class VaccineCatalogLruCache {

    private val map = object : LinkedHashMap<String, CacheEntry>(
        1,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, CacheEntry>
        ): Boolean = false
    }

    data class CacheEntry(
        val vaccines: List<Vaccine>,
        val cachedAt: Long = System.currentTimeMillis()
    )

    @Synchronized
    fun put(vaccines: List<Vaccine>) {
        map[CACHE_KEY_CATALOG] = CacheEntry(vaccines)
    }

    @Synchronized
    fun get(maxAgeMs: Long = TTL_MS): List<Vaccine>? {
        val entry = map[CACHE_KEY_CATALOG] ?: return null

        val isStale = System.currentTimeMillis() - entry.cachedAt > maxAgeMs

        if (isStale) {
            map.remove(CACHE_KEY_CATALOG)
            return null
        }

        return entry.vaccines
    }

    @Synchronized
    fun invalidate() {
        map.remove(CACHE_KEY_CATALOG)
    }

    companion object {
        const val CACHE_KEY_CATALOG = "vaccine_catalog"
        const val TTL_MS = 60 * 60 * 1000L
    }
}