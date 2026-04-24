package com.example.petcare.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.hive.HiveCacheManager
import com.example.petcare.data.local.lru.EventLruCache
import com.example.petcare.data.local.mapper.toEntity
import com.example.petcare.data.local.mapper.toEvent
import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.Event
import com.example.petcare.data.network.ApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * EventRepository con tres capas de caché:
 *
 *  getEvents(petId)  → LRU lista → Hive por petId (TTL 5 min) → API → Room
 *  getEvents(owner)  → LRU lista → Hive por ownerId (TTL 5 min) → API → Room
 *  getEvent(id)      → LRU individual → API → Room
 *  create/update/delete → invalida Hive + LRU correspondientes
 */
class EventRepository(
    private val api: ApiService,
    private val context: Context,
    private val hive: HiveCacheManager,
    private val externalScope: CoroutineScope,
    private val lru: EventLruCache = EventLruCache(context)
) {

    private val eventDao = AppDatabase.getInstance(context).eventDao()
    private val gson     = Gson()

    // ─────────────────────────────────────────────────────────────────────────
    // Conectividad
    // ─────────────────────────────────────────────────────────────────────────

    private fun isOnline(): Boolean {
        val cm  = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Clave de caché — compartida entre LRU y Hive para mantener consistencia
    // ─────────────────────────────────────────────────────────────────────────

    private fun listCacheKey(petId: String?, ownerId: String?): String = when {
        petId   != null -> "pet_$petId"
        ownerId != null -> "owner_$ownerId"
        else            -> "all"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET EVENTS — lista de eventos por pet u owner
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getEvents(
        petId: String? = null,
        ownerId: String? = null
    ): Result<List<Event>> {

        val online = isOnline()
        val key    = listCacheKey(petId, ownerId)

        return if (online) {

            // ── Capa 1: LRU (memoria, instantáneo) ───────────────────────────
            val lruCached = lru.getList(key)
            if (lruCached != null) {
                android.util.Log.d("LRU_CACHE", "HIT lista key=$key")
                return Result.success(lruCached)
            }

            // ── Capa 2: Hive (disco, TTL 5 min) ──────────────────────────────
            val hiveCached = when {
                petId   != null -> hive.getEvents(petId)
                ownerId != null -> hive.getEventsByOwner(ownerId)
                else            -> null
            }
            val isFresh = when {
                petId   != null -> hive.isEventsFresh(petId)
                ownerId != null -> hive.isEventsByOwnerFresh(ownerId)
                else            -> false
            }

            if (hiveCached != null && isFresh) {
                android.util.Log.d("HIVE_CACHE", "HIT fresco - events (Hive) key=$key")
                return runCatching {
                    val type = object : TypeToken<List<Event>>() {}.type
                    val events: List<Event> = gson.fromJson(hiveCached, type)
                    lru.putList(key, events)   // poblar LRU para próximos accesos
                    events
                }
            }

            if (hiveCached != null && !isFresh) {
                android.util.Log.d("HIVE_CACHE", "Expirado - Stale-While-Revalidate key=$key")
                val cachedEvents = runCatching {
                    val type = object : TypeToken<List<Event>>() {}.type
                    val events: List<Event> = gson.fromJson(hiveCached, type)
                    lru.putList(key, events)
                    events
                }
                externalScope.launch { refreshListFromApi(petId, ownerId) }
                return cachedEvents
            }

            // ── Capa 3: API ───────────────────────────────────────────────────
            android.util.Log.d("HIVE_CACHE", "MISS - events va a la API key=$key")
            runCatching {
                fetchListFromApi(petId, ownerId)
            }.recoverCatching { e ->
                android.util.Log.d("EVENT_REPO", "API falló: ${e.message} - Room fallback")
                getListFromRoom(petId, ownerId)
            }

        } else {
            android.util.Log.d("EVENT_REPO", "Offline - events desde Room")
            runCatching { getListFromRoom(petId, ownerId) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET EVENT — evento individual
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getEvent(eventId: String): Result<Event> {
        val online = isOnline()

        // LRU para evento individual (EventDetailsScreen abre/cierra el mismo evento)
        val lruCached = lru.getEvent(eventId)
        if (lruCached != null) {
            android.util.Log.d("LRU_CACHE", "HIT evento individual eventId=$eventId")
            return Result.success(lruCached)
        }

        return if (online) {
            runCatching {
                val event = api.getEvent(eventId).body() ?: error("Event not found")
                lru.putEvent(eventId, event)
                eventDao.upsertAll(listOf(event.toEntity()))
                event
            }.recoverCatching {
                val roomEvent = eventDao.getById(eventId)?.toEvent()
                    ?: error("Event not found locally")
                lru.putEvent(eventId, roomEvent)
                roomEvent
            }
        } else {
            runCatching {
                val roomEvent = eventDao.getById(eventId)?.toEvent()
                    ?: error("Event not found offline")
                lru.putEvent(eventId, roomEvent)
                roomEvent
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE EVENT
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun createEvent(request: CreateEventRequest): Result<Event> {
        val online = isOnline()

        return if (online) {
            runCatching {
                val response = api.createEvent(request)
                if (!response.isSuccessful) {
                    error(parseApiError(response.errorBody()?.string(), response.code(), "create event"))
                }
                val event = response.body() ?: error("Failed to create event — empty response")

                eventDao.upsertAll(listOf(event.toEntity()))

                // Invalida Hive (por petId) + LRU lista
                hive.invalidateEvents(request.petId)
                lru.invalidateList(listCacheKey(request.petId, null))
                android.util.Log.d("LRU_CACHE", "LRU lista + Hive invalidados tras crear (pet=${request.petId})")

                event
            }
        } else {
            runCatching {
                val tempId = "local_event_${System.currentTimeMillis()}"
                val entity = com.example.petcare.data.local.entity.EventEntity(
                    id            = tempId,
                    petId         = request.petId,
                    ownerId       = request.ownerId,
                    title         = request.title,
                    eventType     = request.eventType.uppercase(),
                    date          = request.date,
                    price         = request.price,
                    provider      = request.provider,
                    clinic        = request.clinic,
                    description   = request.description,
                    followUpDate  = request.followUpDate,
                    synced        = false,
                    pendingDelete = false
                )
                eventDao.upsertAll(listOf(entity))
                android.util.Log.d("EVENT_REPO", "Evento guardado offline con id=$tempId")
                entity.toEvent()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE EVENT
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun updateEvent(
        eventId: String,
        title: String,
        description: String,
        provider: String,
        clinic: String,
        price: Double?,
        date: String
    ): Result<Event> {
        val online = isOnline()

        return if (online) {
            runCatching {
                val body = buildMap<String, Any?> {
                    put("title",       title)
                    put("description", description)
                    put("provider",    provider)
                    put("clinic",      clinic)
                    put("date",        date)
                    if (price != null) put("price", price)
                }
                val response = api.updateEvent(eventId, body)
                if (!response.isSuccessful) {
                    error(parseApiError(response.errorBody()?.string(), response.code(), "update event"))
                }
                val event = response.body() ?: error("Failed to update event — empty response")

                eventDao.upsertAll(listOf(event.toEntity()))

                // Invalida Hive + LRU lista + LRU evento individual
                hive.invalidateEvents(event.petId)
                lru.invalidateList(listCacheKey(event.petId, null))
                lru.invalidateEvent(eventId)
                android.util.Log.d("LRU_CACHE", "LRU lista+evento + Hive invalidados tras actualizar eventId=$eventId")

                event
            }
        } else {
            runCatching {
                val existing = eventDao.getById(eventId)
                    ?: error("Event not found offline")

                eventDao.updateEvent(
                    id           = eventId,
                    title        = title,
                    eventType    = existing.eventType,
                    date         = date,
                    price        = price,
                    provider     = provider,
                    clinic       = clinic,
                    description  = description,
                    followUpDate = existing.followUpDate
                )

                lru.invalidateEvent(eventId)
                android.util.Log.d("EVENT_REPO", "Evento actualizado offline id=$eventId")

                eventDao.getById(eventId)?.toEvent()
                    ?: error("Event not found after offline update")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE EVENT
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        val online = isOnline()

        // Obtener petId ANTES de eliminar (Room ya no lo tendrá después)
        val petId = eventDao.getById(eventId)?.petId
        android.util.Log.d("EVENT_REPO", "Eliminando eventId=$eventId petId=$petId")

        return if (online) {
            try {
                val response = api.deleteEvent(eventId)

                if (response.isSuccessful || response.code() == 204) {
                    eventDao.deleteById(eventId)
                    petId?.let { invalidateBothCaches(it, eventId) }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete event — HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                val message = e.message ?: ""
                // Workaround: Django devuelve body en 204, Retrofit lanza excepción
                if (message.contains("204") && message.contains("Content-Length")) {
                    eventDao.deleteById(eventId)
                    petId?.let { invalidateBothCaches(it, eventId) }
                    Result.success(Unit)
                } else {
                    Result.failure(e)
                }
            }
        } else {
            runCatching {
                eventDao.markPendingDelete(eventId)
                petId?.let { invalidateBothCaches(it, eventId) }
                android.util.Log.d("EVENT_REPO", "Evento marcado para eliminar offline id=$eventId")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADD DOCUMENT
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun addDocument(
        eventId: String,
        fileName: String,
        fileUri: String?
    ): Result<Event> = runCatching {
        val body = buildMap<String, Any?> {
            put("fileName", fileName)
            if (fileUri != null) put("fileUri", fileUri)
        }
        val event = api.addEventDocument(eventId, body).body()
            ?: error("Failed to add document — empty response")

        // Actualizar LRU del evento individual con los documentos nuevos
        lru.putEvent(eventId, event)
        event
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun fetchListFromApi(petId: String?, ownerId: String?): List<Event> {
        val response = api.getEvents(petId = petId, ownerId = ownerId)
        if (!response.isSuccessful) error("Failed — HTTP ${response.code()}")
        val events = response.body().orEmpty()
        val json   = gson.toJson(events)
        val key    = listCacheKey(petId, ownerId)

        when {
            petId   != null -> {
                hive.putEvents(petId, json)
                android.util.Log.d("HIVE_CACHE", "Guardando ${events.size} events en Hive (pet=$petId)")
            }
            ownerId != null -> {
                hive.putEventsByOwner(ownerId, json)
                android.util.Log.d("HIVE_CACHE", "Guardando ${events.size} events en Hive (owner=$ownerId)")
            }
        }

        lru.putList(key, events)
        eventDao.upsertAll(events.map { it.toEntity() })
        return events
    }

    private suspend fun refreshListFromApi(petId: String?, ownerId: String?) {
        runCatching {
            fetchListFromApi(petId, ownerId)
            android.util.Log.d("HIVE_CACHE", "Background refresh events completado")
        }.onFailure {
            android.util.Log.d("HIVE_CACHE", "Background refresh falló: ${it.message}")
        }
    }

    private suspend fun getListFromRoom(petId: String?, ownerId: String?): List<Event> = when {
        petId   != null -> eventDao.getForPetSync(petId).map { it.toEvent() }
        ownerId != null -> eventDao.getForOwnerSync(ownerId).map { it.toEvent() }
        else            -> emptyList()
    }

    /** Invalida Hive y LRU coordinados — siempre juntos para evitar inconsistencias */
    private fun invalidateBothCaches(petId: String, eventId: String) {
        hive.invalidateEvents(petId)
        lru.invalidateList(listCacheKey(petId, null))
        lru.invalidateEvent(eventId)
        android.util.Log.d("LRU_CACHE", "🗑️ LRU lista+evento + Hive invalidados (petId=$petId eventId=$eventId)")
    }

    private fun parseApiError(errorBody: String?, code: Int, action: String): String {
        if (errorBody.isNullOrBlank()) return "Failed to $action — HTTP $code"
        return runCatching {
            val json = JSONObject(errorBody)
            json.optString("error").ifBlank { json.optString("message") }.ifBlank { errorBody }
        }.getOrDefault(errorBody).let { "Failed to $action — HTTP $code: $it" }
    }
}