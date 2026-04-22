package com.example.petcare.ui.screens.petprofile.events

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.Event
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.util.EventDateUtils
import com.example.petcare.util.FirebaseDocumentUploader
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.normalizeForCommit
import com.example.petcare.util.sanitizeForEditing
import com.example.petcare.util.trimToNullIfBlank
import com.example.petcare.util.validateCommittedInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventDetailsUiState(
    val event: Event? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingDoc: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false,
    val isEditing: Boolean = false,
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

    fun load(eventId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            FeatureExecutionTracker.track("Load Event Details") {
                RepositoryProvider.eventRepository.getEvent(eventId)
            }.fold(
                onSuccess = { event ->
                    val (appDate, appTime) = EventDateUtils.splitToAppDateTime(event.date)
                    _uiState.value = _uiState.value.copy(
                        event           = event,
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
    fun addDocument(context: Context, petId: String, uri: Uri) {
        val eventId = _uiState.value.event?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingDoc = true, error = null)

            FirebaseDocumentUploader
                .uploadEventDocument(context, uri, petId, eventId)
                .fold(
                    onSuccess = { uploaded ->
                        RepositoryProvider.eventRepository.addDocument(
                            eventId  = eventId,
                            fileName = uploaded.fileName,
                            fileUri  = uploaded.downloadUrl
                        ).fold(
                            onSuccess = { updatedEvent ->
                                _uiState.value = _uiState.value.copy(
                                    event          = updatedEvent,
                                    isUploadingDoc = false
                                )
                            },
                            onFailure = { e ->
                                _uiState.value = _uiState.value.copy(
                                    isUploadingDoc = false,
                                    error = "Uploaded but failed to save: ${e.message}"
                                )
                            }
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isUploadingDoc = false,
                            error          = "Upload failed: ${e.message}"
                        )
                    }
                )
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
