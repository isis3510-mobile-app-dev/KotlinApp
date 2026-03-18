package com.example.petcare.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.Event
import com.example.petcare.data.model.Pet
import com.example.petcare.data.model.Vaccination
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpcomingVaccine(
    val vaccineName: String,
    val petName: String,
    val petId: String,
    val dueDate: String,
    val daysUntilDue: Long
)

data class HomeUiState(
    val userName: String = "",
    val userId: String = "",
    val pets: List<Pet> = emptyList(),
    val recentEvents: List<Event> = emptyList(),
    val upcomingVaccines: List<UpcomingVaccine> = emptyList(),
    val overdueVaccinesCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun setUserInfo(name: String, userId: String) {
        _state.value = _state.value.copy(userName = name, userId = userId)
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            RepositoryProvider.petRepository.getPets().fold(
                onSuccess = { pets ->
                    // Events for all pets in parallel
                    val eventResults = pets.map { pet ->
                        async {
                            RepositoryProvider.eventRepository
                                .getEvents(petId = pet.id)
                                .getOrElse { emptyList() }
                        }
                    }.awaitAll().flatten()

                    // Upcoming vaccines — next 30 days + overdue
                    val today     = java.time.LocalDate.now()
                    val upcoming  = mutableListOf<UpcomingVaccine>()
                    var overdueCount = 0

                    pets.forEach { pet ->
                        pet.vaccinations.forEach { vacc ->
                            val dueDateStr = vacc.nextDueDate ?: return@forEach
                            try {
                                val dueDate = java.time.LocalDate.parse(dueDateStr.take(10))
                                val days    = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate)
                                when {
                                    days < 0  -> overdueCount++   // overdue
                                    days <= 30 -> upcoming.add(   // due within 30 days
                                        UpcomingVaccine(
                                            vaccineName  = vacc.vaccineId.take(8),
                                            petName      = pet.name,
                                            petId        = pet.id,
                                            dueDate      = dueDateStr.take(10),
                                            daysUntilDue = days
                                        )
                                    )
                                }
                            } catch (_: Exception) { /* skip malformed date */ }
                        }
                    }

                    _state.value = _state.value.copy(
                        pets                 = pets,
                        recentEvents         = eventResults
                            .sortedByDescending { it.date }
                            .take(5),
                        upcomingVaccines     = upcoming.sortedBy { it.daysUntilDue },
                        overdueVaccinesCount = overdueCount,
                        isLoading            = false
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to load data"
                    )
                }
            )
        }
    }
}