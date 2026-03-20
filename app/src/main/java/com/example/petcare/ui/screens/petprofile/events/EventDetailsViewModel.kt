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
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.enforceMaxLength
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
                        editTitle       = enforceMaxLength(event.title, InputTextLimits.EVENT_TITLE),
                        editDescription = enforceMaxLength(event.description, InputTextLimits.NOTES),
                        editProvider    = enforceMaxLength(event.provider, InputTextLimits.PROVIDER_OR_CLINIC),
                        editClinic      = enforceMaxLength(event.clinic, InputTextLimits.PROVIDER_OR_CLINIC),
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

    fun setTitle(v: String)       { _uiState.value = _uiState.value.copy(editTitle = enforceMaxLength(v, InputTextLimits.EVENT_TITLE)) }
    fun setDescription(v: String) { _uiState.value = _uiState.value.copy(editDescription = enforceMaxLength(v, InputTextLimits.NOTES)) }
    fun setProvider(v: String)    { _uiState.value = _uiState.value.copy(editProvider = enforceMaxLength(v, InputTextLimits.PROVIDER_OR_CLINIC)) }
    fun setClinic(v: String)      { _uiState.value = _uiState.value.copy(editClinic = enforceMaxLength(v, InputTextLimits.PROVIDER_OR_CLINIC)) }
    fun setPrice(v: String)       { _uiState.value = _uiState.value.copy(editPrice = v) }
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

        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true, error = null)
            FeatureExecutionTracker.track("Edit Event") {
                RepositoryProvider.eventRepository.updateEvent(
                    eventId     = event.id,
                    title       = s.editTitle.trim(),
                    description = s.editDescription.trim(),
                    provider    = s.editProvider.trim(),
                    clinic      = s.editClinic.trim(),
                    price       = s.editPrice.toDoubleOrNull(),
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
