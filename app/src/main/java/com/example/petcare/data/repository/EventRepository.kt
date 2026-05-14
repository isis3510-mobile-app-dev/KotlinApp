package com.example.petcare.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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
import com.example.petcare.data.model.AddDocumentRequest
import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.Event
import com.example.petcare.data.model.PendingEventDocument
import com.example.petcare.data.network.ApiService
import com.example.petcare.data.worker.SyncWorker
import com.example.petcare.util.FirebaseDocumentUploader
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import android.util.Log
import kotlinx.coroutines.tasks.await
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
            ExistingWorkPolicy.APPEND_OR_REPLACE,
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

                val pendingCreateIds = eventDao.getPendingCreatesForMerge().map { it.id }.toSet()
                val pendingDeleteIds = eventDao.getPendingDeletesForMerge().map { it.id }.toSet() // ← agregar

                remoteEvents
                    .map { it.toEntity() }
                    .filter { it.id !in pendingCreateIds }
                    .filter { it.id !in pendingDeleteIds }  // ← nunca pisar pendientes de borrado
                    .let { eventDao.upsertAll(it) }

                if (petId != null) hive.putEvents(petId, gson.toJson(remoteEvents))
                lru.putList(key, remoteEvents)
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
                    attachedDocumentsJson = "[]",
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
                if (response.isSuccessful) {
                    val remote = response.body() ?: error("Empty event body")
                    eventDao.upsert(remote.toEntity())
                    remote
                } else {
                    // Server failed — fall back to Room (handles local_ev_ IDs and sync race conditions)
                    eventDao.getById(eventId)?.toEvent() ?: error("Not found: ${response.code()}")
                }
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
                val currentEvent = getEvent(eventId).getOrThrow()
                val normalizedDocuments = currentEvent.attachedDocuments.map { doc ->
                    mapOf(
                        "documentId" to ensureObjectId(doc.documentId ?: doc.id),
                        "fileName" to doc.fileName,
                        "fileUri" to doc.fileUri
                    )
                }
                val body = mutableMapOf<String, Any?>(
                    "title" to title,
                    "description" to description,
                    "provider" to provider,
                    "clinic" to clinic,
                    "date" to date,
                    "attachedDocuments" to normalizedDocuments
                )
                if (price != null) body["price"] = price
                val response = api.updateEvent(eventId, body)
                if (!response.isSuccessful) error("Update fail: ${response.code()}")
                val event = response.body() ?: error("Empty update response")
                eventDao.upsert(event.toEntity())
                invalidateBothCaches(event.petId, eventId)
                event
            }
        } else {
            runCatching {
                val existing = eventDao.getById(eventId) ?: error("Not found")
                eventDao.updateEventPending(
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
                // Evict list LRU immediately so the card reflects the local change
                lru.invalidateList(listCacheKey(existing.petId, null))
                lru.invalidateEvent(eventId)
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
                val deleteAccepted = try {
                    val response = api.deleteEvent(eventId)
                    response.isSuccessful || response.code() == 204 || response.code() == 404
                } catch (e: Exception) {
                    if (isInvalid204ContentLengthError(e)) {
                        true
                    } else {
                        throw e
                    }
                }
                if (!deleteAccepted) {
                    error("Delete fail")
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
        val body = AddDocumentRequest(fileName = fileName, fileUri = fileUri)
        val response = api.addEventDocument(eventId, body)
        if (!response.isSuccessful) error("Doc add fail: ${response.code()}")
        val event = response.body() ?: error("Doc add empty body")
        eventDao.upsert(event.toEntity())
        lru.putEvent(eventId, event)
        event
    }

    suspend fun deleteEventDocument(eventId: String, documentId: String): Result<Event> {
        if (!isOnline()) return Result.failure(
            Exception("No internet connection. Document delete will be available when you're back online.")
        )
        return runCatching {
            val response = api.deleteEventDocument(eventId, documentId)
            if (!response.isSuccessful) error("Delete document failed: ${response.code()}")
            val event = response.body() ?: error("Empty response after document delete")
            eventDao.upsert(event.toEntity())
            clearHiddenEventDocuments(event.petId, eventId)
            lru.putEvent(eventId, event)
            event
        }
    }

    suspend fun deleteEventDocumentByContent(
        eventId: String,
        fileName: String,
        fileUri: String?
    ): Result<Event> {
        if (!isOnline()) return Result.failure(
            Exception("No internet connection. Document delete will be available when you're back online.")
        )
        return runCatching {
            invalidateEventLru(eventId)
            val current = getEvent(eventId).getOrThrow()
            val remaining = current.attachedDocuments.filterNot { doc ->
                val sameUri = !fileUri.isNullOrBlank() && doc.fileUri == fileUri
                val sameName = doc.fileName == fileName
                sameUri || sameName
            }
            if (remaining.size == current.attachedDocuments.size) {
                error("Document not found on server for delete fallback.")
            }

            fileUri?.takeIf { it.startsWith("http", ignoreCase = true) }?.let { url ->
                runCatching { Firebase.storage.getReferenceFromUrl(url).delete().await() }
            }

            val docsPayload = remaining.map { doc ->
                mapOf(
                    "documentId" to (doc.documentId ?: doc.id),
                    "fileName" to doc.fileName,
                    "fileUri" to doc.fileUri
                )
            }
            val body = mapOf("attachedDocuments" to docsPayload)
            val response = api.updateEvent(eventId, body)
            if (!response.isSuccessful) error("Delete document fallback failed: ${response.code()}")
            val updated = response.body() ?: error("Empty response after document fallback delete")
            eventDao.upsert(updated.toEntity())
            clearHiddenEventDocuments(updated.petId, eventId)
            lru.putEvent(eventId, updated)
            updated
        }
    }

    fun removePendingEventDocument(petId: String, eventId: String, pendingDocId: String) {
        val remaining = getPendingEventDocuments(petId, eventId).filter { it.id != pendingDocId }
        if (remaining.isEmpty()) hive.invalidatePendingEventDocuments(petId, eventId)
        else hive.putPendingEventDocuments(petId, eventId, gson.toJson(remaining))
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
            "DOC_UPLOAD",
            "Queued pending event document id=$id petId=$petId eventId=$eventId file=${localFile.absolutePath}"
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
            Log.e("DOC_UPLOAD", "Failed to parse pending event documents: ${it.message}", it)
            emptyList()
        }
    }

    suspend fun syncPendingEventDocuments(
        petId: String,
        eventId: String
    ): Result<Int> = runCatching {
        if (!isOnline()) {
            Log.d("DOC_UPLOAD", "Pending document sync skipped offline petId=$petId eventId=$eventId")
            return@runCatching 0
        }

        val pendingDocs = getPendingEventDocuments(petId, eventId)
        if (pendingDocs.isEmpty()) return@runCatching 0

        Log.d(
            "DOC_UPLOAD",
            "Pending document sync start petId=$petId eventId=$eventId count=${pendingDocs.size}"
        )
        val remaining = pendingDocs.toMutableList()
        var synced = 0

        pendingDocs.forEach { pending ->
            runCatching {
                val uploaded = FirebaseDocumentUploader
                    .uploadEventDocument(context, Uri.parse(pending.localUri), petId, eventId)
                    .getOrThrow()

                addDocument(
                    eventId,
                    pending.fileName,
                    uploaded.downloadUrl
                ).getOrThrow()

                remaining.remove(pending)
                deleteLocalPendingFile(pending.localUri)
                synced++
                Log.d("DOC_UPLOAD", "Pending document synced id=${pending.id} fileName=${pending.fileName}")
            }.onFailure {
                Log.e("DOC_UPLOAD", "Pending document sync failed id=${pending.id}: ${it.message}", it)
            }
        }

        if (remaining.isEmpty()) {
            hive.invalidatePendingEventDocuments(petId, eventId)
        } else {
            hive.putPendingEventDocuments(petId, eventId, gson.toJson(remaining))
        }
        if (synced > 0) {
            invalidateBothCaches(petId, eventId)
            Log.d("DOC_UPLOAD", "Invalidated event caches after pending document sync petId=$petId synced=$synced")
        }
        synced
    }

    private fun deleteLocalPendingFile(localUri: String) {
        runCatching {
            val path = Uri.parse(localUri).path ?: return
            File(path).delete()
        }
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

    fun invalidateLruForPet(petId: String) {
        hive.invalidateEvents(petId)
        lru.invalidateList(listCacheKey(petId, null))
    }

    fun getHiddenEventDocumentKeys(
        petId: String,
        eventId: String
    ): Set<String> {
        val json = hive.getHiddenEventDocuments(petId, eventId) ?: return emptySet()
        return runCatching {
            gson.fromJson(json, Array<String>::class.java).toSet()
        }.getOrElse { emptySet() }
    }

    fun hideEventDocumentLocally(
        petId: String,
        eventId: String,
        fileName: String,
        fileUri: String?
    ) {
        val key = "${fileName.trim()}|${fileUri.orEmpty().trim()}"
        val updated = getHiddenEventDocumentKeys(petId, eventId).toMutableSet().apply { add(key) }
        hive.putHiddenEventDocuments(petId, eventId, gson.toJson(updated.toList()))
    }

    fun clearHiddenEventDocuments(petId: String, eventId: String) {
        hive.invalidateHiddenEventDocuments(petId, eventId)
    }

    fun invalidateEventLru(eventId: String) {
        lru.invalidateEvent(eventId)
    }

    private fun isInvalid204ContentLengthError(error: Throwable): Boolean {
        val message = error.message.orEmpty()
        return message.contains("HTTP 204 had non-zero Content-Length", ignoreCase = true)
    }

    private fun ensureObjectId(raw: String?): String {
        val cleaned = raw.orEmpty().trim()
        val isObjectId = cleaned.matches(Regex("^[a-fA-F0-9]{24}$"))
        if (isObjectId) return cleaned.lowercase()
        return UUID.randomUUID().toString().replace("-", "").take(24).lowercase()
    }
}
