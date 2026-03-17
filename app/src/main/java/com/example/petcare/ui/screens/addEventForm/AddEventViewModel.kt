package com.example.petcare.ui.screens.addEventForm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddEventFormState(
    val petId: String   = "",   // set from navigation
    val ownerId: String = "",   // set from AuthViewModel.userProfile.id
    // Step 1
    val title: String     = "",
    val date: String      = "",  // dd/MM/yyyy
    val time: String      = "",
    val eventType: String = "checkup",
    // Step 2
    val description: String = "",
    val price: String       = "",
    val provider: String    = "",
    val clinic: String      = "",
    // Step 3
    val followUpDate: String    = "",
    val reminderEnabled: Boolean = false,
    // UI
    val isLoading: Boolean = false,
    val error: String?     = null
)

class AddEventViewModel : ViewModel() {

    private val _state = MutableStateFlow(AddEventFormState())
    val state: StateFlow<AddEventFormState> = _state.asStateFlow()

    fun setPetId(v: String)            { _state.value = _state.value.copy(petId = v) }
    fun setOwnerId(v: String)          { _state.value = _state.value.copy(ownerId = v) }
    fun setTitle(v: String)            { _state.value = _state.value.copy(title = v) }
    fun setDate(v: String)             { _state.value = _state.value.copy(date = v) }
    fun setTime(v: String)             { _state.value = _state.value.copy(time = v) }
    fun setEventType(v: String)        { _state.value = _state.value.copy(eventType = v) }
    fun setDescription(v: String)      { _state.value = _state.value.copy(description = v) }
    fun setPrice(v: String)            { _state.value = _state.value.copy(price = v) }
    fun setProvider(v: String)         { _state.value = _state.value.copy(provider = v) }
    fun setClinic(v: String)           { _state.value = _state.value.copy(clinic = v) }
    fun setFollowUpDate(v: String)     { _state.value = _state.value.copy(followUpDate = v) }
    fun setReminderEnabled(v: Boolean) { _state.value = _state.value.copy(reminderEnabled = v) }

    /** Calls POST /api/events/ and invokes [onSuccess] with the new eventId. */
    fun submit(onSuccess: (eventId: String) -> Unit) {
        val s = _state.value
        if (s.petId.isBlank() || s.title.isBlank() || s.date.isBlank()) {
            _state.value = s.copy(error = "Pet, title and date are required")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)
            val request = CreateEventRequest(
                petId       = s.petId,
                ownerId     = s.ownerId,
                title       = s.title.trim(),
                eventType   = s.eventType.trim().lowercase(),
                date        = toIso(s.date),
                price       = s.price.toDoubleOrNull(),
                provider    = s.provider.trim(),
                clinic      = s.clinic.trim(),
                description = s.description.trim(),
                followUpDate = s.followUpDate.takeIf { it.isNotBlank() }?.let { toIso(it) }
            )
            RepositoryProvider.eventRepository.createEvent(request).fold(
                onSuccess = { event ->
                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess(event.id)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Error")
                }
            )
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun reset()      { _state.value = AddEventFormState() }

    private fun toIso(date: String): String = try {
        val p = date.split("/")
        if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}T00:00:00Z" else date
    } catch (_: Exception) { date }
}