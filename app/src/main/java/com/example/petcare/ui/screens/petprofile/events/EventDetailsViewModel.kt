package com.example.petcare.ui.screens.petprofile.events

import androidx.lifecycle.ViewModel
import com.example.petcare.data.model.EventType
import com.example.petcare.data.model.MedicalEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EventDetailsUiState(
    val event: MedicalEvent? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class EventDetailsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EventDetailsUiState(isLoading = true))
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()

    // For now we mock the data load since there's no repository passed in yet.
    // In a real implementation this would fetch by eventId.
    init {
        loadMockEvent()
    }

    private fun loadMockEvent() {
        val mockEvent = MedicalEvent(
            id = "1",
            petId = "1",
            title = "Checkup",
            eventType = EventType.CHECKUP,
            price = 120.0,
            provider = "Dr. Smith",
            clinic = "Happy Paws Clinic",
            date = "Nov 19, 2024",
            description = "Annual wellness exam. All vitals normal. Weight stable at 28.5kg.",
            followUpDate = "Nov 19, 2025"
        )
        
        _uiState.value = EventDetailsUiState(event = mockEvent, isLoading = false)
    }

    fun onDeleteClicked() {
        // Implementation
    }

    fun onEditClicked() {
        // Implementation
    }

    fun onAddNoteClicked() {
        // Implementation
    }

    fun onAttachDocumentClicked() {
        // Implementation
    }
}
