package com.example.petcare.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.Event
import com.example.petcare.data.model.GroupedSuggestion
import com.example.petcare.data.model.Pet
import com.example.petcare.data.model.PetSuggestion
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.util.EventDateUtils
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
    val vaccinationId: String,   // ← NUEVO: _id del embedded Vaccination
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
    val topAlert: GroupedSuggestion? = null,
    val totalAlertCount: Int = 0,
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

            FeatureExecutionTracker.track("Load Home Data") {
                RepositoryProvider.petRepository.getPets()
            }.fold(
                onSuccess = { pets ->

                    val catalogMap = RepositoryProvider.petRepository
                        .getVaccineCatalog()
                        .getOrElse { emptyList() }
                        .associateBy { it.id }

                    val eventResults = pets.map { pet ->
                        async {
                            RepositoryProvider.eventRepository
                                .getEvents(petId = pet.id)
                                .getOrElse { emptyList() }
                        }
                    }.awaitAll().flatten()

                    val allSuggestions: List<PetSuggestion> = pets.map { pet ->
                        async {
                            RepositoryProvider.petRepository
                                .getPetSmart(pet.id)
                                .getOrElse { emptyList() }
                                .map { PetSuggestion(pet.id, pet.name, it) }
                        }
                    }.awaitAll().flatten()

                    val today    = java.time.LocalDate.now()
                    val upcoming = mutableListOf<UpcomingVaccine>()
                    var overdueCount = 0

                    pets.forEach { pet ->
                        pet.vaccinations.forEach { vacc ->
                            val dueDateStr = vacc.nextDueDate ?: return@forEach
                            try {
                                val dueDate = java.time.LocalDate.parse(dueDateStr.take(10))
                                val days    = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate)
                                when {
                                    days < 0   -> overdueCount++
                                    days <= 30 -> upcoming.add(
                                        UpcomingVaccine(
                                            vaccineName   = catalogMap[vacc.vaccineId]?.name
                                                ?: vacc.vaccineId.take(8),
                                            petName       = pet.name,
                                            petId         = pet.id,
                                            vaccinationId = vacc.id,   // ← el _id embebido
                                            dueDate       = dueDateStr.take(10),
                                            daysUntilDue  = days
                                        )
                                    )
                                }
                            } catch (_: Exception) { }
                        }
                    }

                    val criticalGrouped = allSuggestions
                        .groupBy { it.suggestion.title }
                        .map { (title, items) ->
                            GroupedSuggestion(
                                vaccineTitle = title,
                                type = when {
                                    items.any { it.suggestion.type == "danger" }  -> "danger"
                                    items.any { it.suggestion.type == "warning" } -> "warning"
                                    else -> "info"
                                },
                                pets    = items.map { it.petName }.distinct(),
                                message = items.first().suggestion.message
                            )
                        }
                        .sortedBy { when (it.type) { "danger" -> 0; "warning" -> 1; else -> 2 } }

                    _state.value = _state.value.copy(
                        pets                 = pets,
                        recentEvents         = eventResults
                            .filter { EventDateUtils.isTodayOrFuture(it.date) }
                            .sortedBy { EventDateUtils.parseEventInstant(it.date) ?: java.time.Instant.MAX }
                            .take(5),
                        upcomingVaccines     = upcoming.sortedBy { it.daysUntilDue },
                        overdueVaccinesCount = overdueCount,
                        topAlert             = criticalGrouped.firstOrNull(),
                        totalAlertCount      = criticalGrouped.size,
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
