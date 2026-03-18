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
    val grouped: List<GroupedSuggestion> = emptyList(),
    val filteredByPet: List<GroupedSuggestion> = emptyList(),
    val filterPetId: String? = null,
    val filterPetName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class NotificationsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun load(filterPetId: String? = null, filterPetName: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val pets = RepositoryProvider.petRepository.getPets()
                .getOrElse { emptyList() }

            val allSuggestions: List<PetSuggestion> = pets
                .map { pet ->
                    async {
                        RepositoryProvider.petRepository
                            .getPetSmart(pet.id)
                            .getOrElse { emptyList() }
                            .map { suggestion ->
                                PetSuggestion(
                                    petId      = pet.id,
                                    petName    = pet.name,
                                    suggestion = suggestion
                                )
                            }
                    }
                }
                .awaitAll()
                .flatten()

            val grouped = groupByVaccine(allSuggestions)

            val filtered = if (filterPetId != null) {
                groupByVaccine(allSuggestions.filter { it.petId == filterPetId })
            } else grouped

            _uiState.value = NotificationsUiState(
                grouped        = grouped,
                filteredByPet  = filtered,
                filterPetId    = filterPetId,
                filterPetName  = filterPetName,
                isLoading      = false
            )
        }
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