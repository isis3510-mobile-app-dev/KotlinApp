package com.example.petcare.ui.screens.petprofile.vaccines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.AddDocumentRequest
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineFilterStatus
import com.example.petcare.ui.screens.petprofile.components.vaccines.VaccineRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VaccineDetailsUiState(
    val vaccine: VaccineRecord? = null,
    val petId: String = "",
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false,
    val isEditing: Boolean = false,
    val editAdministeredBy: String = "",
    val editNextDueDate: String = "",
    val editLotNumber: String = ""
)

class VaccineDetailsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VaccineDetailsUiState(isLoading = true))
    val uiState: StateFlow<VaccineDetailsUiState> = _uiState.asStateFlow()

    fun load(petId: String, vaccineId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, petId = petId, error = null)

            // Load catalog to resolve vaccineId → human name
            val catalogMap = RepositoryProvider.petRepository
                .getVaccineCatalog()
                .getOrElse { emptyList() }
                .associateBy { it.id }

            RepositoryProvider.petRepository.getPet(petId).fold(
                onSuccess = { pet ->
                    val vacc = pet.vaccinations.find { it.id == vaccineId }
                    if (vacc == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Vaccine record not found for the given id"
                        )
                        return@fold
                    }
                    val status = when (vacc.status.lowercase()) {
                        "overdue"  -> VaccineFilterStatus.OVERDUE
                        "upcoming" -> VaccineFilterStatus.UPCOMING
                        else       -> VaccineFilterStatus.COMPLETED
                    }
                    val record = VaccineRecord(
                        id                   = vacc.id,
                        name                 = catalogMap[vacc.vaccineId]?.name ?: vacc.vaccineId.take(8),
                        provider             = vacc.administeredBy,
                        dateGiven            = vacc.dateGiven.take(10),
                        nextDueDate          = vacc.nextDueDate?.take(10),
                        lotNumber            = vacc.lotNumber.ifBlank { null },
                        status               = status,
                        attachedDocumentName = vacc.attachedDocuments.firstOrNull()?.fileName
                    )
                    _uiState.value = _uiState.value.copy(
                        vaccine            = record,
                        isLoading          = false,
                        editAdministeredBy = vacc.administeredBy,
                        editNextDueDate    = vacc.nextDueDate?.take(10) ?: "",
                        editLotNumber      = vacc.lotNumber
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun deleteVaccine() {
        val petId         = _uiState.value.petId
        val vaccinationId = _uiState.value.vaccine?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            RepositoryProvider.petRepository.deleteVaccination(
                petId         = petId,
                vaccinationId = vaccinationId
            ).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isDeleting = false, isDeleted = true) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isDeleting = false, error = e.message) }
            )
        }
    }

    fun startEditing()  { _uiState.value = _uiState.value.copy(isEditing = true,  error = null) }
    fun cancelEditing() { _uiState.value = _uiState.value.copy(isEditing = false, error = null) }

    fun setAdministeredBy(v: String) { _uiState.value = _uiState.value.copy(editAdministeredBy = v) }
    fun setNextDueDate(v: String)    { _uiState.value = _uiState.value.copy(editNextDueDate = v) }
    fun setLotNumber(v: String)      { _uiState.value = _uiState.value.copy(editLotNumber = v) }

    fun saveEdits() {
        val petId         = _uiState.value.petId
        val vaccinationId = _uiState.value.vaccine?.id ?: return
        val s             = _uiState.value
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true, error = null)
            RepositoryProvider.petRepository.updateVaccination(
                petId          = petId,
                vaccinationId  = vaccinationId,
                administeredBy = s.editAdministeredBy,
                nextDueDate    = s.editNextDueDate.takeIf { it.isNotBlank() },
                lotNumber      = s.editLotNumber
            ).fold(
                onSuccess = { _ ->
                    _uiState.value = _uiState.value.copy(
                        isSaving  = false,
                        isEditing = false,
                        vaccine   = _uiState.value.vaccine?.copy(
                            provider    = s.editAdministeredBy,
                            nextDueDate = s.editNextDueDate.takeIf { it.isNotBlank() },
                            lotNumber   = s.editLotNumber.takeIf { it.isNotBlank() }
                        )
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
                }
            )
        }
    }

    fun addDocument(fileName: String, fileUri: String?) {
        val petId     = _uiState.value.petId
        val vaccineId = _uiState.value.vaccine?.id ?: return
        viewModelScope.launch {
            RepositoryProvider.petRepository.addVaccinationDocument(
                petId, vaccineId, AddDocumentRequest(fileName = fileName, fileUri = fileUri)
            ).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(error = null) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}