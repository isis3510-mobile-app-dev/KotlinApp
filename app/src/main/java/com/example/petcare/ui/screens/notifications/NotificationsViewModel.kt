package com.example.petcare.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.GroupedSuggestion
import com.example.petcare.data.model.PetSuggestion
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val allGrouped: List<GroupedSuggestion> = emptyList(),
    val displayed: List<GroupedSuggestion> = emptyList(),
    val availablePets: List<PetFilterChip> = emptyList(),
    val selectedPetId: String? = null,
    val filterPetId: String? = null,
    val filterPetName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class PetFilterChip(
    val petId: String,
    val petName: String,
    val alertCount: Int
)

class NotificationsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()
    private var allSuggestions: List<PetSuggestion> = emptyList()

    fun load(filterPetId: String? = null, filterPetName: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val pets = RepositoryProvider.petRepository.getPets()
                .getOrElse { emptyList() }

            allSuggestions = pets
                .map { pet ->
                    async {
                        RepositoryProvider.petRepository
                            .getPetSmart(pet.id)
                            .getOrElse { emptyList() }
                            .map { PetSuggestion(pet.id, pet.name, it) }
                    }
                }
                .awaitAll()
                .flatten()

            val chips = allSuggestions
                .groupBy { it.petId }
                .map { (petId, items) ->
                    PetFilterChip(
                        petId      = petId,
                        petName    = items.first().petName,
                        alertCount = items.size
                    )
                }
                .sortedBy { it.petName }

            val initialFilter = filterPetId
            val displayed = applyFilter(initialFilter)

            _uiState.value = NotificationsUiState(
                allGrouped     = groupByVaccine(allSuggestions),
                displayed      = displayed,
                availablePets  = chips,
                selectedPetId  = initialFilter,
                filterPetId    = filterPetId,
                filterPetName  = filterPetName,
                isLoading      = false
            )

        }
    }

    fun onPetFilterSelected(petId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedPetId = petId,
            displayed     = applyFilter(petId)
        )
    }

    private fun applyFilter(petId: String?): List<GroupedSuggestion> {
        val filtered = if (petId != null) {
            allSuggestions.filter { it.petId == petId }
        } else {
            allSuggestions
        }
        return groupByVaccine(filtered)
    }

    private fun groupByVaccine(suggestions: List<PetSuggestion>): List<GroupedSuggestion> {
        return suggestions
            .groupBy { it.suggestion.title }
            .map { (title, items) ->
                GroupedSuggestion(
                    vaccineTitle = title,
                    type         = worstType(items.map { it.suggestion.type }),
                    pets         = items.map { it.petName }.distinct(),
                    message      = items.first().suggestion.message
                )
            }
            .sortedBy { typeOrder(it.type) }
    }

    private fun typeOrder(type: String) = when (type) {
        "danger"  -> 0
        "warning" -> 1
        else      -> 2
    }

    private fun worstType(types: List<String>) = when {
        types.any { it == "danger" }  -> "danger"
        types.any { it == "warning" } -> "warning"
        else                          -> "info"
    }
}