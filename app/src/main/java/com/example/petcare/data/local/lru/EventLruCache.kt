package com.example.petcare.data.local.lru

import android.app.ActivityManager
import android.content.Context
import android.util.LruCache
import com.example.petcare.data.model.Event

/**
 * LRU Cache en memoria para eventos.
 *
 * Cachea DOS tipos de datos con claves distintas:
 *
 *   1. Listas  →  clave: "pet_<petId>" / "owner_<ownerId>"
 *      Beneficia: CalendarScreen, HealthRecordsScreen, HomeScreen
 *      Rol: evitar deserializar JSON de Hive en cada acceso de la misma sesión
 *
 *   2. Eventos individuales  →  clave: "event_<eventId>"
 *      Beneficia: EventDetailsScreen (abre/cierra el mismo evento repetidamente)
 *      Rol: evitar llamada a la API o a Room en la misma sesión
 *
 * ¿Por qué no usar solo Hive (TTL)?
 *   - Hive lee SharedPreferences del disco y deserializa JSON en cada acceso.
 *   - LRU guarda objetos ya deserializados en memoria → acceso instantáneo.
 *   - Hive persiste entre sesiones; LRU es la capa más rápida dentro de la sesión.
 *   - LRU invalida por CAPACIDAD (cuando se llena); Hive invalida por TIEMPO (TTL).
 *
 * Cálculo del maxSize — regla 1/8 del heap (literatura Android):
 *   Fuente: Android Developer Guide "Caching Bitmaps"
 *   https://developer.android.com/topic/performance/graphics/cache-bitmap
 *   "As a general rule, use no more than 1/8th of the available application memory
 *   for an in-memory cache."
 *
 *   maxSize (KB) = (ActivityManager.memoryClass × 1024) / 8
 *   Ejemplo con heap de 256 MB:  256 × 1024 / 8 = 32.768 KB
 *   Se adapta automáticamente al dispositivo (gama baja vs alta).
 *
 * Cálculo del sizeOf en KB:
 *   Medir en KB (no en entradas) refleja el costo real de memoria.
 *   Un Event sin documentos ≈ 0.5 KB; con 5 documentos ≈ 3 KB.
 *   Campos de texto: 2 bytes/char (UTF-16).
 *   Documento adjunto: ~500 bytes por referencia de URL/path.
 *   Overhead del objeto Java: 200 bytes.
 */
class EventLruCache(context: Context) {

    companion object {
        private const val TAG = "LRU_CACHE"

        fun calculateMaxSizeKb(context: Context): Int {
            val am     = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val heapMb = am.memoryClass               // MB asignados al proceso
            val maxKb  = (heapMb * 1024) / 8          // 1/8 del heap en KB
            android.util.Log.d(
                TAG,
                "Heap: ${heapMb}MB → maxSize LRU: ${maxKb}KB (regla 1/8)"
            )
            return maxKb
        }
    }

    private val maxSizeKb = calculateMaxSizeKb(context)

    private val cache = object : LruCache<String, Any>(maxSizeKb) {

        @Suppress("UNCHECKED_CAST")
        override fun sizeOf(key: String, value: Any): Int {
            return when (value) {
                is List<*> -> {
                    val events    = value as List<Event>
                    val totalBytes = events.sumOf { estimateEventBytes(it) }
                    val sizeKb    = maxOf(1, totalBytes / 1024)
                    android.util.Log.d(
                        TAG,
                        "sizeOf lista key=$key → ${events.size} events, ${totalBytes}B ≈ ${sizeKb}KB"
                    )
                    sizeKb
                }
                is Event -> {
                    val bytes  = estimateEventBytes(value)
                    val sizeKb = maxOf(1, bytes / 1024)
                    android.util.Log.d(
                        TAG,
                        "sizeOf evento key=$key → docs=${value.attachedDocuments.size}, ${bytes}B ≈ ${sizeKb}KB"
                    )
                    sizeKb
                }
                else -> 1
            }
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Any, newValue: Any?) {
            if (evicted) {
                android.util.Log.d(
                    TAG,
                    "♻️ EVICTION key=$key — cache lleno, expulsado por LRU policy | " +
                            "size=${size()}KB/${maxSize()}KB"
                )
            } else {
                android.util.Log.d(TAG, "REMOVED key=$key (invalidado manualmente)")
            }
        }
    }

    // ── Listas ────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    fun getList(key: String): List<Event>? {
        val result = cache.get(key) as? List<Event>
        logAccess("lista", key, result != null)
        return result
    }

    fun putList(key: String, events: List<Event>) {
        cache.put(key, events)
        android.util.Log.d(
            TAG,
            "PUT lista key=$key (${events.size} events) | size=${cache.size()}KB/${cache.maxSize()}KB"
        )
    }

    fun invalidateList(key: String) {
        cache.remove(key)
        android.util.Log.d(TAG, "Invalidated lista key=$key")
    }

    // ── Eventos individuales ──────────────────────────────────────────────

    fun getEvent(eventId: String): Event? {
        val result = cache.get("event_$eventId") as? Event
        logAccess("evento", "event_$eventId", result != null)
        return result
    }

    fun putEvent(eventId: String, event: Event) {
        cache.put("event_$eventId", event)
        android.util.Log.d(
            TAG,
            "PUT evento eventId=$eventId | size=${cache.size()}KB/${cache.maxSize()}KB"
        )
    }

    fun invalidateEvent(eventId: String) {
        cache.remove("event_$eventId")
        android.util.Log.d(TAG, "Invalidated evento eventId=$eventId")
    }

    // ── Limpieza ──────────────────────────────────────────────────────────

    fun evictAll() {
        cache.evictAll()
        android.util.Log.d(TAG, "Cache LRU limpiado completamente")
    }

    // ── Métricas ──────────────────────────────────────────────────────────

    fun hitRate(): Double {
        val total = cache.hitCount() + cache.missCount()
        return if (total == 0) 0.0 else cache.hitCount().toDouble() / total
    }

    /**
     * Resumen legible — útil para imprimir en logs durante la demo de la rúbrica.
     * Ejemplo: LRU Stats | maxSize=32768KB (1/8 de 256MB) | size=4KB |
     *          hits=12 misses=3 | hitRate=80.0% | evictions=0
     */
    fun stats(): String {
        val heapMb = maxSizeKb * 8 / 1024
        return "LRU Stats | " +
                "maxSize=${maxSizeKb}KB (1/8 de ${heapMb}MB heap) | " +
                "size=${cache.size()}KB | " +
                "hits=${cache.hitCount()} misses=${cache.missCount()} | " +
                "hitRate=${"%.1f".format(hitRate() * 100)}% | " +
                "evictions=${cache.evictionCount()}"
    }

    fun snapshot(): Map<String, Any> = cache.snapshot()

    // ── Helpers privados ──────────────────────────────────────────────────

    private fun estimateEventBytes(event: Event): Int {
        val titleBytes    = event.title.length        * 2
        val descBytes     = event.description.length  * 2
        val providerBytes = event.provider.length     * 2
        val clinicBytes   = event.clinic.length       * 2
        val docsBytes     = event.attachedDocuments.size * 500
        val overheadBytes = 200
        return titleBytes + descBytes + providerBytes + clinicBytes + docsBytes + overheadBytes
    }

    private fun logAccess(tipo: String, key: String, hit: Boolean) {
        android.util.Log.d(
            TAG,
            "${if (hit) " HIT" else " MISS"} $tipo key=$key | " +
                    "size=${cache.size()}KB/${cache.maxSize()}KB | " +
                    "hits=${cache.hitCount()} misses=${cache.missCount()} | " +
                    "hitRate=${"%.1f".format(hitRate() * 100)}%"
        )
    }
}