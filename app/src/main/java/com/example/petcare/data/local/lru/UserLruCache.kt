package com.example.petcare.data.local.lru

import com.example.petcare.data.model.User


class UserLruCache(private val capacity: Int = 1) {

    private val map = object : LinkedHashMap<String, CacheEntry>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>): Boolean {
            return size > capacity
        }
    }

    data class CacheEntry(
        val user: User,
        val cachedAt: Long = System.currentTimeMillis()
    )

    @Synchronized
    fun put(key: String, user: User) {
        map[key] = CacheEntry(user)
    }

    @Synchronized
    fun get(key: String, maxAgeMs: Long = TTL_MS): User? {
        val entry = map[key] ?: return null
        val age = System.currentTimeMillis() - entry.cachedAt
        return if (age <= maxAgeMs) entry.user else {
            map.remove(key)
            null
        }
    }

    @Synchronized
    fun invalidate(key: String) {
        map.remove(key)
    }

    @Synchronized
    fun clear() {
        map.clear()
    }

    companion object {
        const val CACHE_KEY_ME = "me"
        /** 5 minutes — stale after this even if the device is offline */
        const val TTL_MS = 5 * 60 * 1000L
    }
}