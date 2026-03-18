package com.example.petcare.ui.screens.addVaccineForm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.AddVaccinationRequest
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddVaccineFormState(
    val petId: String = "",          // set from navigation before opening the form
    // Step 1
    val vaccineName: String  = "",
    val dateGiven: String    = "",   // dd/MM/yyyy  (from DatePicker)
    val productName: String  = "",
    val manufacturer: String = "",
    // Step 2
    val administeredBy: String = "",
    val lotNumber: String      = "",
    // Step 3
    val nextDueDate: String = "",    // dd/MM/yyyy  (optional)
    // UI
    val isLoading: Boolean = false,
    val error: String?     = null
)

class AddVaccineViewModel : ViewModel() {

    private val _state = MutableStateFlow(AddVaccineFormState())
    val state: StateFlow<AddVaccineFormState> = _state.asStateFlow()

    fun setPetId(v: String)          { _state.value = _state.value.copy(petId = v) }
    fun setVaccineName(v: String)    { _state.value = _state.value.copy(vaccineName = v) }
    fun setDateGiven(v: String)      { _state.value = _state.value.copy(dateGiven = v) }
    fun setProductName(v: String)    { _state.value = _state.value.copy(productName = v) }
    fun setManufacturer(v: String)   { _state.value = _state.value.copy(manufacturer = v) }
    fun setAdministeredBy(v: String) { _state.value = _state.value.copy(administeredBy = v) }
    fun setLotNumber(v: String)      { _state.value = _state.value.copy(lotNumber = v) }
    fun setNextDueDate(v: String)    { _state.value = _state.value.copy(nextDueDate = v) }

    /**
     * Calls POST /api/pets/{petId}/vaccinations/.
     * vaccineId uses a placeholder ObjectId until a vaccine catalog is integrated.
     */
    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.petId.isBlank() || s.vaccineName.isBlank() || s.dateGiven.isBlank()) {
            _state.value = s.copy(error = "Pet, vaccine name and date are required")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)
            val request = AddVaccinationRequest(
                vaccineId      = "000000000000000000000000",  // placeholder
                dateGiven      = toIso(s.dateGiven),
                nextDueDate    = s.nextDueDate.takeIf { it.isNotBlank() }?.let { toIso(it) },
                lotNumber      = s.lotNumber.trim(),
                status         = "completed",
                administeredBy = s.administeredBy.trim()
            )
            RepositoryProvider.petRepository.addVaccination(s.petId, request).fold(
                onSuccess = { _state.value = _state.value.copy(isLoading = false); onSuccess() },
                onFailure = { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Error")
                }
            )
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun reset()      { _state.value = AddVaccineFormState() }

    /** Converts dd/MM/yyyy → yyyy-MM-ddT00:00:00Z */
    private fun toIso(date: String): String = try {
        val p = date.split("/")
        if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}T00:00:00Z" else date
    } catch (_: Exception) { date }
}