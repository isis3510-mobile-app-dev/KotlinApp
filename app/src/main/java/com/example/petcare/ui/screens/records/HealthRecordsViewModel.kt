package com.example.petcare.ui.screens.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.ui.components.MedicalEventData
import com.example.petcare.ui.components.VaccineListItemData
import com.example.petcare.util.EventDateUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.petcare.R

data class VaccineListItemWithIds(
    val data: VaccineListItemData,
    val petId: String,
    val vaccinationId: String
)

data class EventListItemWithIds(
    val data: MedicalEventData,
    val petId: String,
    val eventId: String
)

data class HealthRecordsState(
    val vaccines:       List<VaccineListItemWithIds> = emptyList(),
    val events:         List<EventListItemWithIds>   = emptyList(),
    val selectedFilter: String                       = "All",
    val isLoading:      Boolean                      = false,
    val error:          String?                      = null
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

            val petsResult = RepositoryProvider.petRepository.getPets()
            if (petsResult.isFailure) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error     = petsResult.exceptionOrNull()?.message ?: "Failed to load pets"
                )
                return@launch
            }
            val pets = petsResult.getOrDefault(emptyList())

            val catalogMap = RepositoryProvider.petRepository
                .getVaccineCatalog()
                .getOrElse { emptyList() }
                .associateBy { it.id }

            val vaccines = pets.flatMap { pet ->
                pet.vaccinations.map { vacc ->
                    VaccineListItemWithIds(
                        data = VaccineListItemData(
                            vaccineName = catalogMap[vacc.vaccineId]?.name ?: vacc.vaccineId.take(8),
                            petName     = pet.name,
                            clinicName  = vacc.administeredBy.ifBlank { "—" },
                            status      = vacc.status,
                            daysText    = computeDaysText(vacc.nextDueDate, vacc.dateGiven),
                            photoPath   = R.drawable.pet
                        ),
                        petId         = pet.id,
                        // ← vacc.id es el _id del subdocumento (no el vaccineId del catálogo)
                        vaccinationId = vacc.id
                    )
                }
            }

            val eventResults = pets.map { pet ->
                async {
                    RepositoryProvider.eventRepository
                        .getEvents(petId = pet.id)
                        .getOrElse { emptyList() }
                        .map { event ->
                            EventListItemWithIds(
                                data = MedicalEventData(
                                    eventType  = event.eventType.name
                                        .lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    petName    = pet.name,
                                    clinicName = event.clinic.ifBlank { event.provider }.ifBlank { "—" },
                                    date       = parseDate(event.date),
                                    time       = parseTime(event.date),
                                    cost       = event.price?.let { "$${"%.0f".format(it)}" } ?: ""
                                ),
                                petId   = pet.id,
                                eventId = event.id
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
                diff < 0   -> "${-diff}d ago"
                diff == 0L -> "today"
                else       -> "in ${diff}d"
            }
        } catch (e: Exception) {
            nextDueDate.take(10)
        }
    }

    private fun parseDate(iso: String): String {
        return EventDateUtils.splitToAppDateTime(iso).first.ifBlank { iso.take(10) }
    }

    private fun parseTime(iso: String): String {
        return EventDateUtils.splitToAppDateTime(iso).second
    }
}
