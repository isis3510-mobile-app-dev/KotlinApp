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
import com.example.petcare.data.model.EventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.Period

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
    val error: String? = null,
    val lastVetVisits: Map<String, Pair<Int, String>> = emptyMap(), // petId -> (daysSince, date)
    val mostUrgentPet: String = ""
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun setUserInfo(name: String, userId: String) {
        _state.value = _state.value.copy(userName = name, userId = userId)
    }

    fun clearSessionData() {
        _state.value = HomeUiState()
    }

    fun removeDeletedPet(petId: String) {
        val currentPetName = _state.value.pets.firstOrNull { it.id == petId }?.name

        _state.value = _state.value.copy(
            pets = _state.value.pets.filterNot { it.id == petId },
            recentEvents = _state.value.recentEvents.filterNot { it.petId == petId },
            upcomingVaccines = _state.value.upcomingVaccines.filterNot { it.petId == petId },
            topAlert = _state.value.topAlert
                ?.let { alert ->
                    val filteredPets = currentPetName
                        ?.let { deletedPetName -> alert.pets.filterNot { it == deletedPetName } }
                        ?: alert.pets

                    if (filteredPets.isEmpty()) null else alert.copy(pets = filteredPets)
                }
        )
    }

    fun addOrReplacePet(pet: Pet) {
        _state.value = _state.value.copy(
            pets = (_state.value.pets.filterNot { it.id == pet.id } + pet).distinctBy { it.id }
        )
    }

    fun loadData() {

        // Corrutina 2 — Dispatchers.Main
        // Gestiona feedback visual inmediatamente mientras IO trabaja
        // Independiente de IO — no espera sus resultados
        viewModelScope.launch(Dispatchers.Main) {
            android.util.Log.d("MemberB_Thread",
                "Coroutine 2 (Main) started on: ${Thread.currentThread().name}")

            _state.update { it.copy(isLoading = true) }

            var dots = 0
            while (_state.value.isLoading) {
                delay(500)
                dots++
                android.util.Log.d("MemberB_Thread",
                    "Coroutine 2 (Main) tick $dots on: ${Thread.currentThread().name}")
                if (dots >= 10) break
            }

            android.util.Log.d("MemberB_Thread",
                "Coroutine 2 (Main) done on: ${Thread.currentThread().name}")
        }

        // Corrutina 1 — Dispatchers.IO
        // Lee datos del backend, trabajo pesado de red y disco
        viewModelScope.launch(Dispatchers.IO) {
            android.util.Log.d("MemberB_Thread",
                "Coroutine 1 (IO) started on: ${Thread.currentThread().name}")

            _state.value = _state.value.copy(isLoading = true, error = null)

            FeatureExecutionTracker.track("Load Home Data") {
                RepositoryProvider.petRepository.getPets()
            }.fold(
                onSuccess = { pets ->
                    val uniquePets = pets.distinctBy { it.id }

                    android.util.Log.d("MemberB_Thread",
                        "Coroutine 1 (IO) pets loaded on: ${Thread.currentThread().name}")

                    val catalogMap = RepositoryProvider.petRepository
                        .getVaccineCatalog()
                        .getOrElse { emptyList() }
                        .associateBy { it.id }

                    val eventResults = uniquePets.map { pet ->
                        async {
                            RepositoryProvider.eventRepository
                                .getEvents(petId = pet.id)
                                .getOrElse { emptyList() }
                        }
                    }.awaitAll().flatten()

                    val lastVetVisits = mutableMapOf<String, Pair<Int, String>>()
                    var mostUrgentPetId = ""
                    var maxDays = 0

                    pets.forEach { pet ->
                        val petEvents = eventResults.filter { it.petId == pet.id }
                        val lastCheckup = petEvents
                            .filter { it.eventType == EventType.CHECKUP }
                            .maxByOrNull { it.date }

                        if (lastCheckup != null) {
                            val days = calculateDaysSince(lastCheckup.date)
                            lastVetVisits[pet.id] = Pair(days, lastCheckup.date.take(10))

                            if (days > maxDays) {
                                maxDays = days
                                mostUrgentPetId = pet.id
                            }
                        }
                    }

                    val allSuggestions: List<PetSuggestion> = pets.map { pet ->
                        async {
                            RepositoryProvider.petRepository
                                .getPetSmart(pet.id)
                                .getOrElse { emptyList() }
                                .map { PetSuggestion(pet.id, pet.name, it) }
                        }
                    }.awaitAll().flatten()

                    val today    = LocalDate.now()
                    val upcoming = mutableListOf<UpcomingVaccine>()
                    var overdueCount = 0

                    uniquePets.forEach { pet ->
                        pet.vaccinations.forEach { vacc ->
                            val dueDateStr = vacc.nextDueDate ?: return@forEach
                            try {
                                val dueDate = LocalDate.parse(dueDateStr.take(10))
                                val days    = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate)
                                when {
                                    days < 0   -> overdueCount++
                                    days <= 30 -> upcoming.add(
                                        UpcomingVaccine(
                                            vaccineName   = catalogMap[vacc.vaccineId]?.name ?: "Unknown vaccine",
                                            petName       = pet.name,
                                            petId         = pet.id,
                                            vaccinationId = vacc.id,
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

                    android.util.Log.d("MemberB_Thread",
                        "Coroutine 1 (IO) updating state on: ${Thread.currentThread().name}")

                    _state.value = _state.value.copy(
                        pets                 = uniquePets,
                        recentEvents         = eventResults
                            .filter { EventDateUtils.isFuture(it.date) }
                            .sortedBy { EventDateUtils.parseEventInstant(it.date) ?: java.time.Instant.MAX }
                            .take(5),
                        upcomingVaccines     = upcoming.sortedBy { it.daysUntilDue },
                        overdueVaccinesCount = overdueCount,
                        topAlert             = criticalGrouped.firstOrNull(),
                        totalAlertCount      = criticalGrouped.size,
                        lastVetVisits        = lastVetVisits,
                        mostUrgentPet        = mostUrgentPetId,
                        isLoading            = false
                    )
                },
                onFailure = { e ->
                    android.util.Log.d("MemberB_Thread",
                        "Coroutine 1 (IO) failed on: ${Thread.currentThread().name}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to load data"
                    )
                }
            )
        }
    }
}
private fun calculateDaysSince(dateString: String): Int {
    return try {
        val eventDate = LocalDate.parse(dateString.take(10))
        Period.between(eventDate, LocalDate.now()).days
    } catch (e: Exception) {
        0
    }
}
