package com.example.petcare.ui.screens.petprofile.events

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.Event
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.util.FirebaseDocumentUploader
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
                    _uiState.value = _uiState.value.copy(
                        event           = event,
                        isLoading       = false,
                        editTitle       = event.title,
                        editDescription = event.description,
                        editProvider    = event.provider,
                        editClinic      = event.clinic,
                        editPrice       = event.price?.toString() ?: "",
                        editDate        = splitIso(event.date).first,
                        editTime        = splitIso(event.date).second
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

    fun setTitle(v: String)       { _uiState.value = _uiState.value.copy(editTitle = v) }
    fun setDescription(v: String) { _uiState.value = _uiState.value.copy(editDescription = v) }
    fun setProvider(v: String)    { _uiState.value = _uiState.value.copy(editProvider = v) }
    fun setClinic(v: String)      { _uiState.value = _uiState.value.copy(editClinic = v) }
    fun setPrice(v: String)       { _uiState.value = _uiState.value.copy(editPrice = v) }
    fun setDate(v: String)        { _uiState.value = _uiState.value.copy(editDate = v) }
    fun setTime(v: String)        { _uiState.value = _uiState.value.copy(editTime = v) }

    fun saveEdits() {
        val event = _uiState.value.event ?: return
        val s     = _uiState.value
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
                    date        = fromAppFormat(s.editDate, s.editTime)
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

    private fun splitIso(iso: String): Pair<String, String> {
        return try {
            val parts = iso.split("T")
            val datePart = parts[0]
            val timePart = if (parts.size > 1) parts[1].take(5) else "00:00"

            val dP = datePart.split("-")
            val formattedDate = "${dP[2]}/${dP[1]}/${dP[0]}"

            val tP = timePart.split(":")
            val h = tP[0].toInt()
            val m = tP[1].toInt()
            val amPm = if (h >= 12) "PM" else "AM"
            val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
            val formattedTime = "${h12.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')} $amPm"

            formattedDate to formattedTime
        } catch (_: Exception) {
            "" to ""
        }
    }

    private fun fromAppFormat(date: String, time: String): String {
        return try {
            val dP = date.split("/")
            val ymd = "${dP[2]}-${dP[1]}-${dP[0]}"

            val tP = time.split(" ", ":")
            var h = tP[0].toInt()
            val m = tP[1].toInt()
            val ap = tP[2].uppercase()
            if (ap == "PM" && h < 12) h += 12
            if (ap == "AM" && h == 12) h = 0

            "${ymd}T${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:00Z"
        } catch (_: Exception) {
            date
        }
    }
}