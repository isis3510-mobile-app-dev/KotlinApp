package com.example.petcare.ui.screens.calendar


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.Event
import com.example.petcare.data.repository.RepositoryProvider
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

            // 1. Load pets
            val petsResult = RepositoryProvider.petRepository.getPets()
            if (petsResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error     = petsResult.exceptionOrNull()?.message
                )
                return@launch
            }
            val pets     = petsResult.getOrDefault(emptyList())
            val petNames = pets.associate { it.id to it.name }

            // 2. Build vaccination calendar entries
            val vaccinations = pets.flatMap { pet ->
                pet.vaccinations.map { v ->
                    CalendarVaccination(
                        vaccineId   = v.vaccineId,
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
                try { LocalDate.parse(event.date.take(10)) } catch (e: Exception) { null }
            }.toSet()

            val vaccineDates = vaccinations.mapNotNull { v ->
                val dateStr = v.nextDueDate ?: v.dateGiven
                try { LocalDate.parse(dateStr.take(10)) } catch (e: Exception) { null }
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
}