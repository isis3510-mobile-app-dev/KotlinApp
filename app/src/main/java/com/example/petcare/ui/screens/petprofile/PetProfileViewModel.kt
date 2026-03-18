package com.example.petcare.ui.screens.petprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.Event
import com.example.petcare.data.model.Pet
import com.example.petcare.data.model.SuggestionDto
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineFilterStatus
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PetProfileUiState(
    val name: String = "",
    val breed: String = "",
    val species: String = "",
    val age: String = "",
    val weight: String = "",
    val gender: String = "",
    val isHealthy: Boolean = true,
    val color: String = "",
    val microchip: String = "",
    val dateOfBirth: String = "",
    val overdueVaccinesCount: Int = 0,
    val upcomingEventsCount: Int = 0,
    val isNfcSynched: Boolean = false,
    val events: List<Event> = emptyList(),
    val vaccines: List<VaccineRecord> = emptyList(),
    val suggestions: List<SuggestionDto> = emptyList(),
    val vaccineFilter: VaccineFilterStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val photoUrl: String? = null
)

class PetProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PetProfileUiState(isLoading = true))
    val uiState: StateFlow<PetProfileUiState> = _uiState.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    fun loadPet(petId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            RepositoryProvider.petRepository.getPet(petId).fold(
                onSuccess = { pet -> applyPetToState(pet, petId) },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load pet"
                    )
                }
            )
        }
    }

    private suspend fun applyPetToState(pet: Pet, petId: String) {
        // Map backend vaccinations → VaccineRecord UI model
        val vaccines = pet.vaccinations.map { v ->
            val status = when (v.status.lowercase()) {
                "overdue"   -> VaccineFilterStatus.OVERDUE
                "upcoming"  -> VaccineFilterStatus.UPCOMING
                else        -> VaccineFilterStatus.COMPLETED
            }
            VaccineRecord(
                id          = v.vaccineId,
                name        = v.vaccineId.take(8),
                provider    = v.administeredBy,
                dateGiven   = v.dateGiven.take(10),
                nextDueDate = v.nextDueDate?.take(10),
                lotNumber   = v.lotNumber.ifBlank { null },
                status      = status
            )

        }
        // Load events from backend
        val events = mutableListOf<Event>()
        RepositoryProvider.eventRepository.getEvents(petId = petId).fold(
            onSuccess = { list ->
                list.forEach { ev ->
                    events.add(ev.toMedicalEvent())
                }
            },
            onFailure = { /* non-fatal — show empty list */ }
        )

        val suggestions = mutableListOf<SuggestionDto>()
        RepositoryProvider.petRepository.getPetSmart(petId).fold(
            onSuccess = { suggestions.addAll(it) },
            onFailure = { /* non-fatal */ }
        )


        _uiState.value = _uiState.value.copy(
            name                 = pet.name,
            breed                = pet.breed,
            species              = pet.species.replaceFirstChar { it.uppercase() },
            age                  = computeAge(pet.birthDate),
            weight               = if (pet.weight != null) "${pet.weight} kg" else "",
            gender               = pet.gender.replaceFirstChar { it.uppercase() },
            isHealthy            = pet.status.lowercase() == "healthy",
            color                = pet.color,
            microchip            = "",
            dateOfBirth          = pet.birthDate?.take(10) ?: "",
            isNfcSynched         = pet.isNfcSynced,
            overdueVaccinesCount = vaccines.count { it.status == VaccineFilterStatus.OVERDUE },
            upcomingEventsCount  = events.size,
            vaccines             = vaccines,
            events               = events,
            suggestions = suggestions,
            isLoading            = false,
            error                = null,
            photoUrl             = pet.photoUrl
        )

    }

    // ── Tab & filter logic (same as before) ──────────────────────────────────

    fun onTabSelected(index: Int) { _selectedTabIndex.value = index }

    fun onVaccineFilterClick(status: VaccineFilterStatus) {
        val current = _uiState.value.vaccineFilter
        _uiState.value = _uiState.value.copy(
            vaccineFilter = if (current == status) null else status
        )
    }

    fun onVaccineClicked(vaccine: VaccineRecord) {}
    fun onAddEventClicked()  {}
    fun onAddVaccineClicked() {}
    fun onLostModeClicked()  {}
    fun onNfcActiveClicked() {}

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun computeAge(birthDateIso: String?): String {
        if (birthDateIso.isNullOrBlank()) return ""
        return try {
            val parts = birthDateIso.take(10).split("-")
            if (parts.size != 3) return ""
            val birthYear  = parts[0].toInt()
            val birthMonth = parts[1].toInt()
            val now        = java.util.Calendar.getInstance()
            val nowYear    = now.get(java.util.Calendar.YEAR)
            val nowMonth   = now.get(java.util.Calendar.MONTH) + 1
            var years      = nowYear - birthYear
            if (nowMonth < birthMonth) years--
            "$years yrs"
        } catch (_: Exception) { "" }
    }

    fun deletePet(petId: String, onNavigatedBack: () -> Unit) {
        viewModelScope.launch {
            RepositoryProvider.petRepository.deletePet(petId).fold(
                onSuccess = { onNavigatedBack() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }
}

/** Maps the network Event model to the local MedicalEvent UI model. */
private fun Event.toMedicalEvent(): Event {
    return Event(
        id          = id,
        petId       = petId,
        ownerId = ownerId,
        title       = title,
        eventType   = eventType,
        price       = price,
        provider    = provider,
        clinic      = clinic,
        date        = date.take(10),
        description = description,
        followUpDate = followUpDate?.take(10)
    )
}