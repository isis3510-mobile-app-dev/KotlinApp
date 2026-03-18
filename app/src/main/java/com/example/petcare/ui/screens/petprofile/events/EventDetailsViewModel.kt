package com.example.petcare.ui.screens.petprofile.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.Event
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventDetailsUiState(
    val event: Event? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false,
    val isEditing: Boolean = false,
    // Edit fields — pre-filled when editing starts
    val editTitle: String       = "",
    val editDescription: String = "",
    val editProvider: String    = "",
    val editClinic: String      = "",
    val editPrice: String       = ""
)

class EventDetailsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailsUiState(isLoading = true))
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()

    // ── Load ──────────────────────────────────────────────────────────────

    fun load(eventId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            RepositoryProvider.eventRepository.getEvent(eventId).fold(
                onSuccess = { event ->
                    _uiState.value = _uiState.value.copy(
                        event           = event,
                        isLoading       = false,
                        editTitle       = event.title,
                        editDescription = event.description,
                        editProvider    = event.provider,
                        editClinic      = event.clinic,
                        editPrice       = event.price?.toString() ?: ""
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

    // ── Delete ────────────────────────────────────────────────────────────

    fun deleteEvent() {
        val eventId = _uiState.value.event?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            RepositoryProvider.eventRepository.deleteEvent(eventId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        isDeleted  = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        error      = e.message ?: "Failed to delete event"
                    )
                }
            )
        }
    }

    // ── Edit ──────────────────────────────────────────────────────────────

    fun startEditing()  { _uiState.value = _uiState.value.copy(isEditing = true,  error = null) }
    fun cancelEditing() { _uiState.value = _uiState.value.copy(isEditing = false, error = null) }

    fun setTitle(v: String)       { _uiState.value = _uiState.value.copy(editTitle = v) }
    fun setDescription(v: String) { _uiState.value = _uiState.value.copy(editDescription = v) }
    fun setProvider(v: String)    { _uiState.value = _uiState.value.copy(editProvider = v) }
    fun setClinic(v: String)      { _uiState.value = _uiState.value.copy(editClinic = v) }
    fun setPrice(v: String)       { _uiState.value = _uiState.value.copy(editPrice = v) }

    fun saveEdits() {
        val event = _uiState.value.event ?: return
        val s     = _uiState.value
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true, error = null)
            RepositoryProvider.eventRepository.updateEvent(
                eventId     = event.id,
                title       = s.editTitle.trim(),
                description = s.editDescription.trim(),
                provider    = s.editProvider.trim(),
                clinic      = s.editClinic.trim(),
                price       = s.editPrice.toDoubleOrNull()
            ).fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        event     = updated,
                        isSaving  = false,
                        isEditing = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error    = e.message ?: "Failed to save"
                    )
                }
            )
        }
    }

    // ── Documents ─────────────────────────────────────────────────────────

    fun addDocument(fileName: String, fileUri: String?) {
        val eventId = _uiState.value.event?.id ?: return
        viewModelScope.launch {
            RepositoryProvider.eventRepository.addDocument(
                eventId  = eventId,
                fileName = fileName,
                fileUri  = fileUri
            ).fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(event = updated)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            )
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun onAttachDocumentClicked() { /* triggered via filePicker in the screen */ }
    fun onDeleteClicked() = deleteEvent()
    fun onEditClicked()   = startEditing()
}