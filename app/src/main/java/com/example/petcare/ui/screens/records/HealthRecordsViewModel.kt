package com.example.petcare.ui.screens.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.ui.components.MedicalEventData
import com.example.petcare.ui.components.VaccineListItemData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.petcare.R

data class HealthRecordsState(
    val vaccines:       List<VaccineListItemData> = emptyList(),
    val events:         List<MedicalEventData>    = emptyList(),
    val selectedFilter: String                    = "All",
    val isLoading:      Boolean                   = false,
    val error:          String?                   = null
)

class HealthRecordsViewModel : ViewModel() {

    private val _state = MutableStateFlow(HealthRecordsState())
    val state: StateFlow<HealthRecordsState> = _state.asStateFlow()

    fun onFilterSelected(filter: String) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // 1. Load user's pets
            val petsResult = RepositoryProvider.petRepository.getPets()
            if (petsResult.isFailure) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error     = petsResult.exceptionOrNull()?.message ?: "Failed to load pets"
                )
                return@launch
            }
            val pets = petsResult.getOrDefault(emptyList())

            // 2. Build vaccine list from embedded vaccinations in each pet
            val vaccines = pets.flatMap { pet ->
                pet.vaccinations.map { vacc ->
                    val daysText = computeDaysText(vacc.nextDueDate, vacc.dateGiven)
                    VaccineListItemData(
                        vaccineName = vacc.vaccineId.take(8), // until vaccine catalog is integrated
                        petName     = pet.name,
                        clinicName  = vacc.administeredBy.ifBlank { "—" },
                        status      = vacc.status,
                        daysText    = daysText,
                        photoPath   = R.drawable.pet
                    )
                }
            }

            // 3. Load events for all user's pets in parallel
            val eventResults = pets.map { pet ->
                async {
                    RepositoryProvider.eventRepository
                        .getEvents(petId = pet.id)
                        .getOrElse { emptyList() }
                        .map { event ->
                            MedicalEventData(
                                eventType  = event.eventType.name
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                petName    = pet.name,
                                clinicName = event.clinic.ifBlank { event.provider }.ifBlank { "—" },
                                date       = event.date.take(10),
                                cost       = event.price?.let { "$${"%.0f".format(it)}" } ?: ""
                            )
                        }
                }
            }.awaitAll().flatten()

            _state.value = _state.value.copy(
                vaccines  = vaccines,
                events    = eventResults,
                isLoading = false
            )
        }
    }

    private fun computeDaysText(nextDueDate: String?, dateGiven: String): String {
        if (nextDueDate.isNullOrBlank()) return dateGiven.take(10)
        return try {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val due  = formatter.parse(nextDueDate.take(10)) ?: return nextDueDate.take(10)
            val now  = java.util.Date()
            val diff = (due.time - now.time) / (1000 * 60 * 60 * 24)
            when {
                diff < 0  -> "${-diff}d ago"
                diff == 0L -> "today"
                else      -> "in ${diff}d"
            }
        } catch (e: Exception) {
            nextDueDate.take(10)
        }
    }
}