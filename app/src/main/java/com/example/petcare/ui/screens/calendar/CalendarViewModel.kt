package com.example.petcare.ui.screens.calendar


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.Event
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.util.EventDateUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

// Lightweight model for vaccine entries on the calendar
data class CalendarVaccination(
    val vaccineId: String,
    val vaccineName: String,
    val petName: String,
    val dateGiven: String,
    val nextDueDate: String?
)

data class CalendarUiState(
    val events: List<Event> = emptyList(),
    val vaccinations: List<CalendarVaccination> = emptyList(),
    // Pre-computed sets for dot markers on the calendar
    val allEventDates: Set<LocalDate> = emptySet(),
    val allVaccineDates: Set<LocalDate> = emptySet(),
    // Map petId → petName for display
    val petNames: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CalendarViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // 1. Load pets and vaccine catalog
            val petsResult = RepositoryProvider.petRepository.getPets()
            val catalogResult = RepositoryProvider.petRepository.getVaccineCatalog()

            if (petsResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error     = petsResult.exceptionOrNull()?.message
                )
                return@launch
            }
            val pets     = petsResult.getOrDefault(emptyList())
            val petNames = pets.associate { it.id to it.name }
            val catalog  = catalogResult.getOrDefault(emptyList()).associateBy { it.id }

            // 2. Build vaccination calendar entries
            val vaccinations = pets.flatMap { pet ->
                pet.vaccinations.map { v ->
                    CalendarVaccination(
                        vaccineId   = v.vaccineId,
                        vaccineName = resolveVaccineName(v.vaccineName, v.vaccineId, catalog),
                        petName     = pet.name,
                        dateGiven   = v.dateGiven,
                        nextDueDate = v.nextDueDate
                    )
                }
            }

            // 3. Load events for all pets in parallel
            val events = pets.map { pet ->
                async {
                    RepositoryProvider.eventRepository
                        .getEvents(petId = pet.id)
                        .getOrElse { emptyList() }
                }
            }.awaitAll().flatten()

            // 4. Pre-compute marked dates for the calendar dots
            val eventDates = events.mapNotNull { event ->
                EventDateUtils.parseEventDate(event.date)
            }.toSet()

            val vaccineDates = vaccinations.mapNotNull { v ->
                val dateStr = v.nextDueDate ?: v.dateGiven
                EventDateUtils.parseEventDate(dateStr)
            }.toSet()

            _uiState.value = _uiState.value.copy(
                events          = events,
                vaccinations    = vaccinations,
                petNames        = petNames,
                allEventDates   = eventDates,
                allVaccineDates = vaccineDates,
                isLoading       = false
            )
        }
    }

    private fun resolveVaccineName(
        rawName: String?,
        vaccineId: String?,
        catalogMap: Map<String, com.example.petcare.data.model.Vaccine>
    ): String {
        val fromRecord = rawName?.trim().takeUnless { it.isNullOrBlank() }
        if (fromRecord != null) return fromRecord

        val fromCatalog = vaccineId
            ?.trim()
            ?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
            ?.let { catalogMap[it]?.name }
            ?.trim()
            .takeUnless { it.isNullOrBlank() }

        return fromCatalog ?: "Unknown vaccine"
    }
}
