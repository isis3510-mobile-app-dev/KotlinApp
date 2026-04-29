package com.example.petcare.ui.screens.petprofile.events

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.AttachedDocument
import com.example.petcare.data.model.Event
import com.example.petcare.data.network.isOnline
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.util.EventDateUtils
import com.example.petcare.util.FirebaseDocumentUploader
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.PicassoImageCompressor
import com.example.petcare.util.normalizeForCommit
import com.example.petcare.util.sanitizeForEditing
import com.example.petcare.util.trimToNullIfBlank
import com.example.petcare.util.validateCommittedInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EventDetailsUiState(
    val event: Event? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingDoc: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false,
    val isEditing: Boolean = false,
    val petBirthDateIso: String? = null,
    val editTitle: String       = "",
    val editDescription: String = "",
    val editProvider: String    = "",
    val editClinic: String      = "",
    val editPrice: String       = "",
    val editDate: String        = "",
    val editTime: String        = ""
)

class EventDetailsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailsUiState(isLoading = true))
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()

    fun load(eventId: String, petId: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            if (petId.isNotBlank()) {
                RepositoryProvider.eventRepository.syncPendingEventDocuments(petId, eventId)
                    .onSuccess { synced ->
                        if (synced > 0) {
                            Log.d(TAG, "Synced pending event documents before load count=$synced eventId=$eventId")
                        }
                    }
            }
            if (petId.isNotBlank()) {
                RepositoryProvider.petRepository.getPet(petId).onSuccess { pet ->
                    _uiState.value = _uiState.value.copy(petBirthDateIso = pet.birthDate)
                }
            }
            FeatureExecutionTracker.track("Load Event Details") {
                RepositoryProvider.eventRepository.getEvent(eventId)
            }.fold(
                onSuccess = { event ->
                    val (appDate, appTime) = EventDateUtils.splitToAppDateTime(event.date)
                    val displayEvent = event.copy(
                        attachedDocuments = event.attachedDocuments + pendingAttachedDocuments(event.petId, event.id)
                    )
                    _uiState.value = _uiState.value.copy(
                        event           = displayEvent,
                        isLoading       = false,
                        editTitle       = sanitizeForEditing(event.title, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.EVENT_TITLE).value,
                        editDescription = sanitizeForEditing(event.description, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.NOTES).value,
                        editProvider    = sanitizeForEditing(event.provider, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value,
                        editClinic      = sanitizeForEditing(event.clinic, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value,
                        editPrice       = event.price?.toString() ?: "",
                        editDate        = appDate,
                        editTime        = appTime
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to load event"
                    )
                }
            )
        }
    }

    fun deleteEvent() {
        val eventId = _uiState.value.event?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            FeatureExecutionTracker.track("Delete Event") {
                RepositoryProvider.eventRepository.deleteEvent(eventId)
            }.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isDeleting = false, isDeleted = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isDeleting = false, error = e.message)
                }
            )
        }
    }

    fun startEditing()  { _uiState.value = _uiState.value.copy(isEditing = true,  error = null) }
    fun cancelEditing() { _uiState.value = _uiState.value.copy(isEditing = false, error = null) }

    fun setTitle(v: String)       { _uiState.value = _uiState.value.copy(editTitle = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.EVENT_TITLE).value) }
    fun setDescription(v: String) { _uiState.value = _uiState.value.copy(editDescription = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.NOTES).value) }
    fun setProvider(v: String)    { _uiState.value = _uiState.value.copy(editProvider = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value) }
    fun setClinic(v: String)      { _uiState.value = _uiState.value.copy(editClinic = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value) }
    fun setPrice(v: String)       { _uiState.value = _uiState.value.copy(editPrice = sanitizeForEditing(v, InputFieldPolicy.DECIMAL, InputTextLimits.PRICE).value) }
    fun setDate(v: String)        { _uiState.value = _uiState.value.copy(editDate = v) }
    fun setTime(v: String)        { _uiState.value = _uiState.value.copy(editTime = v) }

    fun saveEdits() {
        val event = _uiState.value.event ?: return
        val s     = _uiState.value

        val isoDate = EventDateUtils.toIsoFromAppDateTime(
            appDate = s.editDate,
            appTime = s.editTime,
            fallbackRaw = event.date
        )
        if (isoDate == null) {
            _uiState.value = s.copy(
                isSaving = false,
                error = "Invalid event date/time. Please choose a valid date."
            )
            return
        }
        val selectedDate = EventDateUtils.parseEventDate(isoDate)
        val birthDate = EventDateUtils.parseEventDate(s.petBirthDateIso)
        if (selectedDate != null && birthDate != null && selectedDate.isBefore(birthDate)) {
            _uiState.value = s.copy(
                isSaving = false,
                error = "Event date cannot be before pet's birth date (${s.petBirthDateIso?.take(10)})."
            )
            return
        }

        val validationMessage = listOfNotNull(
            validateCommittedInput(s.editTitle, InputFieldPolicy.GENERAL_TEXT, required = true, maxLength = InputTextLimits.EVENT_TITLE, fieldName = "Title"),
            validateCommittedInput(s.editDescription, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.NOTES),
            validateCommittedInput(s.editProvider, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.PROVIDER_OR_CLINIC),
            validateCommittedInput(s.editClinic, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.PROVIDER_OR_CLINIC),
            validateCommittedInput(s.editPrice, InputFieldPolicy.DECIMAL, maxLength = InputTextLimits.PRICE, fieldName = "Price")
        ).firstOrNull()
        if (validationMessage != null) {
            _uiState.value = s.copy(isSaving = false, error = validationMessage)
            return
        }

        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true, error = null)
            FeatureExecutionTracker.track("Edit Event") {
                RepositoryProvider.eventRepository.updateEvent(
                    eventId     = event.id,
                    title       = normalizeForCommit(s.editTitle, InputFieldPolicy.GENERAL_TEXT),
                    description = normalizeForCommit(s.editDescription, InputFieldPolicy.GENERAL_TEXT),
                    provider    = normalizeForCommit(s.editProvider, InputFieldPolicy.GENERAL_TEXT),
                    clinic      = normalizeForCommit(s.editClinic, InputFieldPolicy.GENERAL_TEXT),
                    price       = normalizeForCommit(s.editPrice, InputFieldPolicy.DECIMAL).trimToNullIfBlank()?.toDoubleOrNull(),
                    date        = isoDate
                )
            }.fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        event     = updated,
                        isSaving  = false,
                        isEditing = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
                }
            )
        }
    }

    /**
     * 1. Sube el archivo a Firebase Storage bajo pets/{petId}/documents/events/{eventId}/
     * 2. Registra la URL pública en el backend Django
     */
    fun addDocument(
        context: Context,
        petId: String,
        uri: Uri,
        mimeType: String? = null,
        fileName: String? = null
    ) {
        val eventId = _uiState.value.event?.id ?: return
        Log.d(TAG, "Detail event document upload requested petId=$petId eventId=$eventId")
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Detail event upload coroutine started thread=${Thread.currentThread().name}")
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isUploadingDoc = true, error = null)
            }

            val resolvedMimeType = mimeType ?: context.contentResolver.getType(uri) ?: "application/octet-stream"
            val resolvedFileName = fileName ?: FirebaseDocumentUploader.getFileName(context, uri)
                ?: "document_${System.currentTimeMillis()}"

            if (!isOnline(context)) {
                queuePendingDocument(uri, petId, eventId, resolvedFileName, resolvedMimeType)
                return@launch
            }

            val prepared = try {
                async(Dispatchers.IO) {
                    PicassoImageCompressor.prepareImageIfNeeded(context, uri, resolvedMimeType, resolvedFileName)
                }.await()
            } catch (e: Exception) {
                Log.e(TAG, "Detail event document preparation failed fileName=$resolvedFileName: ${e.message}", e)
                queuePendingDocument(uri, petId, eventId, resolvedFileName, resolvedMimeType)
                return@launch
            }
            Log.d(TAG, "Detail event document prepared original=$resolvedFileName prepared=${prepared.fileName}")

            FirebaseDocumentUploader
                .uploadEventDocument(context, prepared.uri, petId, eventId)
                .fold(
                    onSuccess = { uploaded ->
                        Log.d(TAG, "Detail event Firebase upload success fileName=${uploaded.fileName}")
                        RepositoryProvider.eventRepository.addDocument(
                            eventId  = eventId,
                            fileName = prepared.fileName,
                            fileUri  = uploaded.downloadUrl
                        ).fold(
                            onSuccess = { updatedEvent ->
                                Log.d(TAG, "Detail event backend metadata saved eventId=$eventId")
                                withContext(Dispatchers.Main) {
                                    _uiState.value = _uiState.value.copy(
                                        event = updatedEvent,
                                        isUploadingDoc = false
                                    )
                                }
                            },
                            onFailure = { e ->
                                Log.e(TAG, "Detail event backend metadata failed eventId=$eventId: ${e.message}", e)
                                withContext(Dispatchers.Main) {
                                    _uiState.value = _uiState.value.copy(
                                        isUploadingDoc = false,
                                        error = "Uploaded but failed to save: ${e.message}"
                                    )
                                }
                            }
                        )
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Detail event Firebase upload failed eventId=$eventId: ${e.message}", e)
                        queuePendingDocument(uri, petId, eventId, resolvedFileName, resolvedMimeType)
                    }
                )
        }
    }

    fun deleteDocument(eventId: String, documentId: String) {
        viewModelScope.launch {
            RepositoryProvider.eventRepository.deleteEventDocument(eventId, documentId)
                .onSuccess { updatedEvent ->
                    _uiState.value = _uiState.value.copy(event = updatedEvent)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    private fun pendingAttachedDocuments(
        petId: String,
        eventId: String
    ): List<AttachedDocument> =
        RepositoryProvider.eventRepository
            .getPendingEventDocuments(petId, eventId)
            .map {
                AttachedDocument(
                    id = "pending_${it.id}",
                    fileName = "${it.fileName} (pending sync)",
                    fileUri = it.localUri
                )
            }

    private suspend fun queuePendingDocument(
        uri: Uri,
        petId: String,
        eventId: String,
        fileName: String,
        mimeType: String
    ) {
        RepositoryProvider.eventRepository
            .queueEventDocument(uri, petId, eventId, fileName, mimeType)
            .fold(
                onSuccess = { pending ->
                    Log.d(TAG, "Detail event document queued locally id=${pending.id} fileName=${pending.fileName}")
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isUploadingDoc = false,
                            error = "No internet connection. Document saved locally and will sync when online.",
                            event = _uiState.value.event?.copy(
                                attachedDocuments = _uiState.value.event?.attachedDocuments.orEmpty() +
                                    AttachedDocument(
                                        id = "pending_${pending.id}",
                                        fileName = "${pending.fileName} (pending sync)",
                                        fileUri = pending.localUri
                                    )
                            )
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Detail event document local queue failed fileName=$fileName: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isUploadingDoc = false,
                            error = "Could not save document locally: ${e.message}"
                        )
                    }
                }
            )
    }

    private companion object {
        const val TAG = "EVENT_DOC_UPLOAD"
    }
}
