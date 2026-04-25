package com.example.petcare.ui.screens.petprofile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.Event
import com.example.petcare.data.model.Pet
import com.example.petcare.data.model.SuggestionDto
import com.example.petcare.data.model.UpdatePetRequest
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineFilterStatus
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineRecord
import com.example.petcare.util.EventDateUtils
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
    val isLost: Boolean = false,
    val color: String = "",
    val dateOfBirth: String = "",
    val overdueVaccinesCount: Int = 0,
    val upcomingEventsCount: Int = 0,
    val isNfcSynched: Boolean = false,
    val events: List<Event> = emptyList(),
    val vaccines: List<VaccineRecord> = emptyList(),
    val vaccineFilter: VaccineFilterStatus? = null,
    val suggestions: List<SuggestionDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val photoUrl: String? = null,
    // ── Extra fields for EditPetBottomSheet ──────────────────────────────────
    val knownAllergies: String = "",
    val defaultVet: String = "",
    val defaultClinic: String = ""
)

class PetProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PetProfileUiState(isLoading = true))
    val uiState: StateFlow<PetProfileUiState> = _uiState.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private var currentPetId: String = ""
    private var isUpdatingLostStatus = false

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadPet(petId: String) {
        currentPetId = petId
        viewModelScope.launch {
            Log.d(TAG, "loadPet start petId=$petId")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            FeatureExecutionTracker.track("Load Pet Profile") {
                RepositoryProvider.petRepository.getPet(petId)
            }.fold(
                onSuccess = { pet ->
                    Log.d(TAG, "loadPet success petId=$petId vaccinations=${pet.vaccinations.size}")
                    applyPetToState(pet, petId)
                },
                onFailure = { e ->
                    Log.e(TAG, "loadPet failed petId=$petId: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to load pet"
                    )
                }
            )
        }
    }

    /** Silent refresh — used after editing or deleting a vaccination/event */
    fun reloadPet() {
        if (currentPetId.isBlank()) return
        viewModelScope.launch {
            Log.d(TAG, "reloadPet start petId=$currentPetId")
            RepositoryProvider.petRepository.getPet(currentPetId).fold(
                onSuccess = { pet ->
                    Log.d(TAG, "reloadPet success petId=$currentPetId vaccinations=${pet.vaccinations.size}")
                    applyPetToState(pet, currentPetId)
                },
                onFailure = {
                    Log.e(TAG, "reloadPet failed petId=$currentPetId: ${it.message}", it)
                }
            )
        }
    }

    private suspend fun applyPetToState(pet: Pet, petId: String) {
        val catalogMap = RepositoryProvider.petRepository
            .getVaccineCatalog()
            .getOrElse { emptyList() }
            .associateBy { it.id }

        val vaccines = pet.vaccinations
            .map { v ->
                val status = when (v.status.lowercase()) {
                    "overdue"  -> VaccineFilterStatus.OVERDUE
                    "upcoming" -> VaccineFilterStatus.UPCOMING
                    else       -> VaccineFilterStatus.COMPLETED
                }
                VaccineRecord(
                    id          = v.id,
                    name        = catalogMap[v.vaccineId]?.name ?: "Unknown vaccine",
                    provider    = v.administeredBy,
                    dateGiven   = v.dateGiven.take(10),
                    nextDueDate = v.nextDueDate?.take(10),
                    lotNumber   = v.lotNumber.ifBlank { null },
                    status      = status,
                    attachedDocuments = v.attachedDocuments.map { doc ->
                        com.example.petcare.data.model.AttachedDocument(
                            id       = doc.id,
                            fileName = doc.fileName,
                            fileUri  = doc.fileUri
                        )
                    }
                )
            }
            .sortedByDescending { it.dateGiven }
        Log.d(TAG, "applyPetToState petId=$petId vaccines=${vaccines.size}")

        val fetchedEvents = mutableListOf<Event>()
        RepositoryProvider.eventRepository.getEvents(petId = petId).fold(
            onSuccess = { list -> fetchedEvents.addAll(list) },
            onFailure = { /* non-fatal */ }
        )
        val events = fetchedEvents.map { it.toMedicalEvent() }
        val upcomingCount = fetchedEvents.count { EventDateUtils.isFuture(it.date) }

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
            isLost               = pet.status.lowercase() == "lost",
            color                = pet.color,
            dateOfBirth          = pet.birthDate?.take(10) ?: "",
            isNfcSynched         = pet.isNfcSynced,
            overdueVaccinesCount = vaccines.count { it.status == VaccineFilterStatus.OVERDUE },
            upcomingEventsCount  = upcomingCount,
            vaccines             = vaccines,
            events               = events,
            suggestions          = suggestions,
            isLoading            = false,
            error                = null,
            photoUrl             = pet.photoUrl,
            // ── Extra fields for the edit sheet ───────────────────────────────
            knownAllergies       = pet.knownAllergies,
            defaultVet           = pet.defaultVet,
            defaultClinic        = pet.defaultClinic
        )
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    fun onTabSelected(index: Int)           { _selectedTabIndex.value = index }
    fun onVaccineFilterClick(status: VaccineFilterStatus) {
        val current = _uiState.value.vaccineFilter
        _uiState.value = _uiState.value.copy(
            vaccineFilter = if (current == status) null else status
        )
    }

    fun onVaccineClicked(vaccine: VaccineRecord) {}
    fun onAddEventClicked()   {}
    fun onAddVaccineClicked() {}
    fun onLostModeClicked() {
        if (currentPetId.isBlank() || isUpdatingLostStatus) return

        val nextStatus = if (_uiState.value.isLost) "healthy" else "lost"
        isUpdatingLostStatus = true

        viewModelScope.launch {
            try {
                FeatureExecutionTracker.track("Toggle Lost Mode") {
                    RepositoryProvider.petRepository.updatePet(
                        petId = currentPetId,
                        request = UpdatePetRequest(status = nextStatus)
                    )
                }.fold(
                    onSuccess = { updatedPet ->
                        applyPetToState(updatedPet, currentPetId)
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            error = e.message ?: "Failed to update lost status"
                        )
                    }
                )
            } finally {
                isUpdatingLostStatus = false
            }
        }
    }
    fun onNfcActiveClicked()  {}

    // ── Delete pet ────────────────────────────────────────────────────────────

    fun deletePet(petId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            FeatureExecutionTracker.track("Delete Pet") {
                RepositoryProvider.petRepository.deletePet(petId)
            }.fold(
                onSuccess = { onDeleted() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun computeAge(birthDateIso: String?): String {
        if (birthDateIso.isNullOrBlank()) return ""
        return try {
            val parts      = birthDateIso.take(10).split("-")
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

    private companion object {
        const val TAG = "PET_PROFILE"
    }
}

private fun Event.toMedicalEvent(): Event = Event(
    id           = id,
    petId        = petId,
    ownerId      = ownerId,
    title        = title,
    eventType    = eventType,
    price        = price,
    provider     = provider,
    clinic       = clinic,
    date         = date,
    description  = description,
    followUpDate = followUpDate
)
