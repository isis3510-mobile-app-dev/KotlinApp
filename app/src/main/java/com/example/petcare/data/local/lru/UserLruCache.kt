package com.example.petcare.data.local.lru

import android.util.Log
import com.example.petcare.data.model.User


class UserLruCache(private val capacity: Int = 1) {

    private val map = object : LinkedHashMap<String, CacheEntry>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>): Boolean {
            val shouldEvict = size > capacity
            if (shouldEvict) {
                Log.d(
                    TAG,
                    "EVICT key=${eldest.key} reason=capacity sizeBefore=$size capacity=$capacity"
                )
            }
            return shouldEvict
        }
    }

    data class CacheEntry(
        val user: User,
        val cachedAt: Long = System.currentTimeMillis()
    )

    @Synchronized
    fun put(key: String, user: User) {
        val replacing = map.containsKey(key)
        map[key] = CacheEntry(user)
        Log.d(
            TAG,
            "PUT key=$key userId=${user.id} replacing=$replacing size=${map.size}/$capacity ttlMs=$TTL_MS"
        )
    }

    @Synchronized
    fun get(key: String, maxAgeMs: Long = TTL_MS): User? {
        val entry = map[key] ?: run {
            Log.d(TAG, "MISS key=$key reason=empty size=${map.size}/$capacity")
            return null
        }
        val age = System.currentTimeMillis() - entry.cachedAt
        return if (age <= maxAgeMs) {
            Log.d(TAG, "HIT key=$key ageMs=$age maxAgeMs=$maxAgeMs size=${map.size}/$capacity")
            entry.user
        } else {
            map.remove(key)
            Log.d(TAG, "STALE_EVICT key=$key ageMs=$age maxAgeMs=$maxAgeMs")
            null
        }
    }

    @Synchronized
    fun invalidate(key: String) {
        val removed = map.remove(key) != null
        Log.d(TAG, "INVALIDATE key=$key existed=$removed size=${map.size}/$capacity")
    }

    @Synchronized
    fun clear() {
        val previousSize = map.size
        map.clear()
        Log.d(TAG, "CLEAR previousSize=$previousSize")
    }

    companion object {
        private const val TAG = "USER_LRU_CACHE"
        const val CACHE_KEY_ME = "me"
        /** 5 minutes - stale after this even if the device is offline */
        const val TTL_MS = 5 * 60 * 1000L
    }
}
