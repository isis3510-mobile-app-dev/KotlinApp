package com.example.petcare.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.entity.EventEntity
import com.example.petcare.data.local.hive.HiveCacheManager
import com.example.petcare.data.local.lru.EventLruCache
import com.example.petcare.data.local.mapper.toEntity
import com.example.petcare.data.local.mapper.toEvent
import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.Event
import com.example.petcare.data.model.PendingEventDocument
import com.example.petcare.data.network.ApiService
import com.example.petcare.data.worker.SyncWorker
import com.example.petcare.util.FirebaseDocumentUploader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.UUID

class EventRepository(
    private val api: ApiService,
    private val context: Context,
    private val hive: HiveCacheManager,
    private val externalScope: CoroutineScope,
    private val lru: EventLruCache = EventLruCache(context)
) {

    private val eventDao = AppDatabase.getInstance(context).eventDao()
    private val gson = Gson()

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        petId != null -> "pet_$petId"
        ownerId != null -> "owner_$ownerId"
        else -> "all"
    }

    suspend fun getEvents(
        petId: String? = null,
        ownerId: String? = null
    ): Result<List<Event>> {
        val online = isOnline()
        val key = listCacheKey(petId, ownerId)

        if (online) {
            lru.getList(key)?.let { return Result.success(mergeEventsWithLocal(it, petId, ownerId)) }

            val hiveCached = when {
                petId != null -> hive.getEvents(petId)
                ownerId != null -> hive.getEventsByOwner(ownerId)
                else -> null
            }
            val isFresh = when {
                petId != null -> hive.isEventsFresh(petId)
                ownerId != null -> hive.isEventsByOwnerFresh(ownerId)
                else -> false
            }

            if (hiveCached != null && isFresh) {
                val type = object : TypeToken<List<Event>>() {}.type
                val events: List<Event> = gson.fromJson(hiveCached, type)
                lru.putList(key, events)
                return Result.success(mergeEventsWithLocal(events, petId, ownerId))
            }
        }

        return if (online) {
            runCatching {
                val response = api.getEvents(petId = petId, ownerId = ownerId)
                if (!response.isSuccessful) error("API fail: ${response.code()}")
                val remoteEvents = response.body().orEmpty()
                eventDao.upsertAll(remoteEvents.map { it.toEntity() })
                if (petId != null) hive.putEvents(petId, gson.toJson(remoteEvents))
                mergeEventsWithLocal(remoteEvents, petId, ownerId)
            }.recoverCatching {
                mergeEventsWithLocal(getListFromRoom(petId, ownerId), petId, ownerId)
            }
        } else {
            runCatching { mergeEventsWithLocal(getListFromRoom(petId, ownerId), petId, ownerId) }
        }
    }

    private suspend fun mergeEventsWithLocal(
        remote: List<Event>,
        petId: String?,
        ownerId: String?
    ): List<Event> {
        val pendingCreates = eventDao.getPendingCreatesForMerge()
            .filter { (petId == null || it.petId == petId) && (ownerId == null || it.ownerId == ownerId) }
            .map { it.toEvent() }

        val merged = remote.toMutableList()
        val remoteIds = remote.map { it.id }.toSet()
        pendingCreates.forEach { if (!remoteIds.contains(it.id)) merged.add(it) }

        val pendingDeletes = eventDao.getPendingDeletesForMerge().map { it.id }.toSet()
        return merged
            .filterNot { pendingDeletes.contains(it.id) }
            .sortedBy { it.date }
    }

    suspend fun createEvent(request: CreateEventRequest): Result<Event> {
        return if (isOnline()) {
            runCatching {
                val response = api.createEvent(request)
                if (!response.isSuccessful) error("Create fail: ${response.code()}")
                val event = response.body() ?: error("Empty response")
                eventDao.upsert(event.toEntity())
                hive.invalidateEvents(request.petId)
                lru.invalidateList(listCacheKey(request.petId, null))
                event
            }
        } else {
            runCatching {
                val tempId = "local_ev_${System.currentTimeMillis()}"
                val entity = EventEntity(
                    id = tempId,
                    petId = request.petId,
                    ownerId = request.ownerId,
                    title = request.title,
                    eventType = request.eventType.uppercase(),
                    date = request.date,
                    price = request.price,
                    provider = request.provider,
                    clinic = request.clinic,
                    description = request.description,
                    followUpDate = request.followUpDate,
                    synced = false,
                    pendingDelete = false,
                    pendingOperation = "CREATE",
                    retryCount = 0,
                    nextRetryAt = 0L
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
                val response = api.getEvent(eventId)
                if (!response.isSuccessful) error("Not found: ${response.code()}")
                response.body() ?: error("Empty event body")
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
        return if (isOnline()) {
            runCatching {
                val body = mapOf(
                    "title" to title,
                    "description" to description,
                    "provider" to provider,
                    "clinic" to clinic,
                    "date" to date,
                    "price" to price
                )
                val response = api.updateEvent(eventId, body)
                if (!response.isSuccessful) error("Update fail: ${response.code()}")
                val event = response.body() ?: error("Empty update response")
                eventDao.upsert(event.toEntity())
                lru.invalidateEvent(eventId)
                event
            }
        } else {
            runCatching {
                val existing = eventDao.getById(eventId) ?: error("Not found")
                eventDao.updateEvent(
                    id = eventId,
                    title = title,
                    eventType = existing.eventType,
                    date = date,
                    price = price,
                    provider = provider,
                    clinic = clinic,
                    description = description,
                    followUpDate = existing.followUpDate
                )
                enqueueSyncWork()
                eventDao.getById(eventId)?.toEvent() ?: error("Error after update")
            }
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        val existing = eventDao.getById(eventId)
        val petId = existing?.petId

        return if (isOnline()) {
            runCatching {
                val response = api.deleteEvent(eventId)
                if (!(response.isSuccessful || response.code() == 204)) {
                    error("Delete fail: ${response.code()}")
                }
                eventDao.deleteById(eventId)
                petId?.let { invalidateBothCaches(it, eventId) }
                Unit
            }
        } else {
            runCatching {
                if (eventId.startsWith("local_ev_")) {
                    eventDao.deleteById(eventId)
                } else {
                    eventDao.markPendingDelete(eventId)
                }
                enqueueSyncWork()
                petId?.let { invalidateBothCaches(it, eventId) }
                Unit
            }
        }
    }

    suspend fun addDocument(eventId: String, fileName: String, fileUri: String?): Result<Event> = runCatching {
        Log.d("EVENT_DOC_UPLOAD", "Saving event document metadata eventId=$eventId fileName=$fileName")
        val body = mapOf("fileName" to fileName, "fileUri" to fileUri)
        val response = api.addEventDocument(eventId, body)
        if (!response.isSuccessful) error("Doc add fail: ${response.code()}")
        val event = response.body() ?: error("Doc add empty body")
        invalidateBothCaches(event.petId, eventId)
        lru.putEvent(eventId, event)
        Log.d(
            "EVENT_DOC_UPLOAD",
            "Backend event document metadata saved eventId=$eventId petId=${event.petId} docs=${event.attachedDocuments.size}"
        )
        event
    }

    suspend fun queueEventDocument(
        sourceUri: Uri,
        petId: String,
        eventId: String,
        fileName: String,
        mimeType: String
    ): Result<PendingEventDocument> = runCatching {
        val id = UUID.randomUUID().toString()
        val safeFileName = fileName.replace(Regex("""[^\w.\-]"""), "_")
        val dir = File(context.filesDir, "pending_event_documents/$petId/$eventId")
        dir.mkdirs()
        val localFile = File(dir, "${id}_$safeFileName")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            localFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read selected document")

        val pending = PendingEventDocument(
            id = id,
            petId = petId,
            eventId = eventId,
            fileName = fileName,
            mimeType = mimeType,
            localUri = Uri.fromFile(localFile).toString()
        )
        val updated = getPendingEventDocuments(petId, eventId) + pending
        hive.putPendingEventDocuments(petId, eventId, gson.toJson(updated))
        Log.d(
            "EVENT_DOC_UPLOAD",
            "Queued pending event document id=$id petId=$petId eventId=$eventId file=${localFile.absolutePath} bytes=${localFile.length()}"
        )
        enqueueSyncWork()
        pending
    }

    fun getPendingEventDocuments(
        petId: String,
        eventId: String
    ): List<PendingEventDocument> {
        val json = hive.getPendingEventDocuments(petId, eventId) ?: return emptyList()
        return runCatching {
            gson.fromJson(json, Array<PendingEventDocument>::class.java).toList()
        }.getOrElse {
            Log.e("EVENT_DOC_UPLOAD", "Failed to parse pending event documents: ${it.message}", it)
            emptyList()
        }
    }

    suspend fun syncPendingEventDocuments(
        petId: String,
        eventId: String
    ): Result<Int> = runCatching {
        if (!isOnline()) {
            Log.d("EVENT_DOC_UPLOAD", "Pending event document sync skipped offline petId=$petId eventId=$eventId")
            return@runCatching 0
        }

        val pendingDocs = getPendingEventDocuments(petId, eventId)
        if (pendingDocs.isEmpty()) return@runCatching 0

        Log.d(
            "EVENT_DOC_UPLOAD",
            "Pending event document sync start petId=$petId eventId=$eventId count=${pendingDocs.size}"
        )
        val remaining = pendingDocs.toMutableList()
        var synced = 0

        pendingDocs.forEach { pending ->
            runCatching {
                val uploaded = FirebaseDocumentUploader
                    .uploadEventDocument(context, Uri.parse(pending.localUri), petId, eventId)
                    .getOrThrow()

                addDocument(
                    eventId = eventId,
                    fileName = pending.fileName,
                    fileUri = uploaded.downloadUrl
                ).getOrThrow()

                remaining.remove(pending)
                deleteLocalPendingFile(pending.localUri)
                synced++
                Log.d("EVENT_DOC_UPLOAD", "Pending event document synced id=${pending.id} fileName=${pending.fileName}")
            }.onFailure {
                Log.e("EVENT_DOC_UPLOAD", "Pending event document sync failed id=${pending.id}: ${it.message}", it)
            }
        }

        if (remaining.isEmpty()) {
            hive.invalidatePendingEventDocuments(petId, eventId)
        } else {
            hive.putPendingEventDocuments(petId, eventId, gson.toJson(remaining))
        }
        if (synced > 0) {
            invalidateBothCaches(petId, eventId)
            Log.d("EVENT_DOC_UPLOAD", "Invalidated event caches after pending document sync petId=$petId eventId=$eventId synced=$synced")
        }
        synced
    }

    private suspend fun getListFromRoom(petId: String?, ownerId: String?): List<Event> = when {
        petId != null -> eventDao.getForPetSync(petId).map { it.toEvent() }
        ownerId != null -> eventDao.getForOwnerSync(ownerId).map { it.toEvent() }
        else -> emptyList()
    }

    private fun invalidateBothCaches(petId: String, eventId: String) {
        hive.invalidateEvents(petId)
        lru.invalidateList(listCacheKey(petId, null))
        lru.invalidateEvent(eventId)
    }

    private fun deleteLocalPendingFile(localUri: String) {
        runCatching {
            val path = Uri.parse(localUri).path ?: return
            File(path).delete()
        }
    }
}
