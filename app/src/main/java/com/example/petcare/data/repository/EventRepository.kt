package com.example.petcare.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.hive.HiveCacheManager
import com.example.petcare.data.local.lru.EventLruCache
import com.example.petcare.data.local.mapper.toEntity
import com.example.petcare.data.local.mapper.toEvent
import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.Event
import com.example.petcare.data.network.ApiService
import com.example.petcare.data.worker.SyncWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

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
    // Conectividad y Sincronización (Patrón Pets)
    // ─────────────────────────────────────────────────────────────────────────

    private fun isOnline(): Boolean {
        val cm  = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun listCacheKey(petId: String?, ownerId: String?): String = when {
        petId   != null -> "pet_$petId"
        ownerId != null -> "owner_$ownerId"
        else            -> "all"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET EVENTS — Con lógica de Mezcla (Merge) para evitar que desaparezcan
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getEvents(
        petId: String? = null,
        ownerId: String? = null
    ): Result<List<Event>> {

        val online = isOnline()
        val key    = listCacheKey(petId, ownerId)

        if (online) {
            // Capa 1: LRU
            val lruCached = lru.getList(key)
            if (lruCached != null) {
                return Result.success(mergeEventsWithLocal(lruCached, petId, ownerId))
            }

            // Capa 2: Hive
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
                val type = object : TypeToken<List<Event>>() {}.type
                val events: List<Event> = gson.fromJson(hiveCached, type)
                lru.putList(key, events)
                return Result.success(mergeEventsWithLocal(events, petId, ownerId))
            }
        }

        // Capa 3: API o Fallback Room
        return if (online) {
            runCatching {
                val response = api.getEvents(petId = petId, ownerId = ownerId)
                if (!response.isSuccessful) error("API Fail")
                val remoteEvents = response.body().orEmpty()

                // Persistencia persistente (Room) y caché volátil (Hive)
                eventDao.upsertAll(remoteEvents.map { it.toEntity().copy(synced = true) })
                if (petId != null) hive.putEvents(petId, gson.toJson(remoteEvents))

                mergeEventsWithLocal(remoteEvents, petId, ownerId)
            }.recoverCatching {
                mergeEventsWithLocal(getListFromRoom(petId, ownerId), petId, ownerId)
            }
        } else {
            runCatching {
                mergeEventsWithLocal(getListFromRoom(petId, ownerId), petId, ownerId)
            }
        }
    }

    /** * Mezcla eventos remotos/caché con los pendientes de subir (synced=0)
     * Esto evita que el evento "se borre" recién creado offline.
     */
    private suspend fun mergeEventsWithLocal(remote: List<Event>, petId: String?, ownerId: String?): List<Event> {
        val unsynced = eventDao.getUnsynced()
            .filter { (petId == null || it.petId == petId) && (ownerId == null || it.ownerId == ownerId) }
            .map { it.toEvent() }

        val merged = remote.toMutableList()
        val remoteIds = remote.map { it.id }.toSet()

        unsynced.forEach { if (!remoteIds.contains(it.id)) merged.add(it) }

        val pendingDeletes = eventDao.getPendingDeletes().map { it.id }.toSet()
        return merged.filterNot { pendingDeletes.contains(it.id) }
            .sortedBy { it.date } // Orden cronológico
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos de acción (CREATE/UPDATE/DELETE) con soporte Offline
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun createEvent(request: CreateEventRequest): Result<Event> {
        return if (isOnline()) {
            runCatching {
                val response = api.createEvent(request)
                if (!response.isSuccessful) error("Create fail")
                val event = response.body() ?: error("Empty response")

                eventDao.upsert(event.toEntity().copy(synced = true))
                hive.invalidateEvents(request.petId)
                lru.invalidateList(listCacheKey(request.petId, null))
                event
            }
        } else {
            runCatching {
                val tempId = "local_ev_${System.currentTimeMillis()}"
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
                eventDao.upsert(entity)
                enqueueSyncWork()
                entity.toEvent()
            }
        }
    }

    suspend fun getEvent(eventId: String): Result<Event> {
        lru.getEvent(eventId)?.let { return Result.success(it) }

        return runCatching {
            val event = if (isOnline()) {
                api.getEvent(eventId).body() ?: error("Not found")
            } else {
                eventDao.getById(eventId)?.toEvent() ?: error("Not found offline")
            }
            lru.putEvent(eventId, event)
            event
        }
    }

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
                val body = mapOf("title" to title, "description" to description, "provider" to provider, "clinic" to clinic, "date" to date, "price" to price)
                val response = api.updateEvent(eventId, body)
                val event = response.body() ?: error("Update fail")
                eventDao.upsert(event.toEntity().copy(synced = true))
                lru.invalidateEvent(eventId)
                event
            }
        } else {
            runCatching {
                val existing = eventDao.getById(eventId) ?: error("Not found")
                eventDao.updateEvent(eventId, title, existing.eventType, date, price, provider, clinic, description, existing.followUpDate)
                enqueueSyncWork()
                eventDao.getById(eventId)?.toEvent() ?: error("Error after update")
            }
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        val petId = eventDao.getById(eventId)?.petId
        return if (isOnline()) {
            runCatching {
                api.deleteEvent(eventId)
                eventDao.deleteById(eventId)
                petId?.let { invalidateBothCaches(it, eventId) }
                Unit
            }
        } else {
            runCatching {
                eventDao.markPendingDelete(eventId)
                enqueueSyncWork()
            }
        }
    }

    suspend fun addDocument(eventId: String, fileName: String, fileUri: String?): Result<Event> = runCatching {
        val body = mapOf("fileName" to fileName, "fileUri" to fileUri)
        val event = api.addEventDocument(eventId, body).body() ?: error("Doc add fail")
        lru.putEvent(eventId, event)
        event
    }

    private suspend fun getListFromRoom(petId: String?, ownerId: String?): List<Event> = when {
        petId   != null -> eventDao.getForPetSync(petId).map { it.toEvent() }
        ownerId != null -> eventDao.getForOwnerSync(ownerId).map { it.toEvent() }
        else            -> emptyList()
    }

    private fun invalidateBothCaches(petId: String, eventId: String) {
        hive.invalidateEvents(petId)
        lru.invalidateList(listCacheKey(petId, null))
        lru.invalidateEvent(eventId)
    }
}