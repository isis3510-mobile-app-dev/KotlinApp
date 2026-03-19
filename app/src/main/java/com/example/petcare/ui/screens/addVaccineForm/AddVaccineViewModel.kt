package com.example.petcare.ui.screens.addVaccineForm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.AddVaccinationRequest
import com.example.petcare.data.model.Vaccine
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddVaccineFormState(
    val petId: String = "",
    // Step 1
    val selectedVaccine: Vaccine? = null,   // the vaccine chosen from the dropdown
    val catalogVaccines: List<Vaccine> = emptyList(),  // loaded from backend
    val isCatalogLoading: Boolean = false,
    val dateGiven: String = "",
    // Step 2
    val administeredBy: String = "",
    val lotNumber: String = "",
    // Step 3
    val nextDueDate: String = "",
    // UI
    val isLoading: Boolean = false,
    val error: String? = null
)

class AddVaccineViewModel : ViewModel() {

    private val _state = MutableStateFlow(AddVaccineFormState())
    val state: StateFlow<AddVaccineFormState> = _state.asStateFlow()

    // Called when the screen first opens so the dropdown has data
    fun loadCatalog(petSpecies: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCatalogLoading = true)
            RepositoryProvider.petRepository.getVaccineCatalog().fold(
                onSuccess = { all ->
                    // Filter by species if we know it, otherwise show all
                    val filtered = if (petSpecies.isBlank()) all
                    else all.filter { vaccine ->
                        vaccine.species.any { it.equals(petSpecies, ignoreCase = true) }
                    }
                    _state.value = _state.value.copy(
                        catalogVaccines  = filtered,
                        isCatalogLoading = false
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(isCatalogLoading = false)
                }
            )
        }
    }

    fun setPetId(v: String)              { _state.value = _state.value.copy(petId = v) }
    fun setSelectedVaccine(v: Vaccine?)  { _state.value = _state.value.copy(selectedVaccine = v) }
    fun setDateGiven(v: String)          { _state.value = _state.value.copy(dateGiven = v) }
    fun setAdministeredBy(v: String)     { _state.value = _state.value.copy(administeredBy = v) }
    fun setLotNumber(v: String)          { _state.value = _state.value.copy(lotNumber = v) }
    fun setNextDueDate(v: String)        { _state.value = _state.value.copy(nextDueDate = v) }

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.petId.isBlank() || s.selectedVaccine == null || s.dateGiven.isBlank()) {
            _state.value = s.copy(error = "Pet, vaccine and date are required")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)
            val request = AddVaccinationRequest(
                vaccineId      = s.selectedVaccine.id,   // ← real catalog ID, not a placeholder
                dateGiven      = toIso(s.dateGiven),
                nextDueDate    = s.nextDueDate.takeIf { it.isNotBlank() }?.let { toIso(it) },
                lotNumber      = s.lotNumber.trim(),
                status         = "completed",
                administeredBy = s.administeredBy.trim()
            )
            RepositoryProvider.petRepository.addVaccination(s.petId, request).fold(
                onSuccess = {
                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Error")
                }
            )
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun reset()      { _state.value = AddVaccineFormState() }

    private fun toIso(date: String): String = try {
        val p = date.split("/")
        if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}T00:00:00Z" else date
    } catch (_: Exception) { date }
}