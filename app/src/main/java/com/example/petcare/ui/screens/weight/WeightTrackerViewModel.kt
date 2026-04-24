package com.example.petcare.ui.screens.weight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.PetCareApplication
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.CreateWeightLogRequest
import com.example.petcare.data.model.UpdateWeightLogRequest
import com.example.petcare.data.model.UpdatePetRequest
import com.example.petcare.data.model.WeightLog
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.util.EventDateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

data class WeightTrackerUiState(
    val petId: String = "",
    val petName: String = "",
    val petBirthDateIso: String? = null,
    val logs: List<WeightLog> = emptyList(),
    val weightInput: String = "",
    val dateInput: String = "",
    val editingLogId: String? = null,
    val selectedRange: WeightGraphRange = WeightGraphRange.WEEK,
    val selectedLogId: String? = null,
    val preferredUnit: String = "kg",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class WeightTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = (application as PetCareApplication).userPreferencesRepository

    private val _uiState = MutableStateFlow(WeightTrackerUiState())
    val uiState: StateFlow<WeightTrackerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.preferredWeightUnit.collect { unit ->
                _uiState.value = _uiState.value.copy(preferredUnit = unit)
            }
        }
    }

    fun load(petId: String) {
        if (petId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, petId = petId)
            preferences.setLastWeightTrackerPetId(petId)

            val petResult = RepositoryProvider.petRepository.getPet(petId)
            val logsResult = RepositoryProvider.weightLogRepository.getWeightLogs(petId)

            val pet = petResult.getOrNull()
            _uiState.value = _uiState.value.copy(
                petName = pet?.name.orEmpty(),
                petBirthDateIso = pet?.birthDate,
                logs = logsResult.getOrDefault(emptyList()),
                selectedLogId = _uiState.value.selectedLogId
                    ?.takeIf { id -> logsResult.getOrDefault(emptyList()).any { it.id == id } },
                isLoading = false,
                error = petResult.exceptionOrNull()?.message ?: logsResult.exceptionOrNull()?.message
            )
        }
    }

    fun setWeightInput(value: String) {
        val normalized = value.filterIndexed { index, ch ->
            ch.isDigit() || (ch == '.' && value.indexOf('.') == index)
        }
        _uiState.value = _uiState.value.copy(weightInput = normalized.take(8), error = null)
    }

    fun setDateInput(value: String) {
        _uiState.value = _uiState.value.copy(dateInput = value, error = null)
    }

    fun setPreferredUnit(unit: String) {
        viewModelScope.launch { preferences.setPreferredWeightUnit(unit) }
    }

    fun setSelectedRange(range: WeightGraphRange) {
        val graphLogs = logsForGraph(_uiState.value.logs, range)
        _uiState.value = _uiState.value.copy(
            selectedRange = range,
            selectedLogId = _uiState.value.selectedLogId?.takeIf { id -> graphLogs.any { it.id == id } }
        )
    }

    fun selectLog(log: WeightLog?) {
        _uiState.value = _uiState.value.copy(selectedLogId = log?.id)
    }

    fun startEditing(log: WeightLog) {
        val unit = _uiState.value.preferredUnit
        val displayWeight = if (unit == "lb") log.weight * 2.20462 else log.weight
        _uiState.value = _uiState.value.copy(
            editingLogId = log.id,
            weightInput = String.format(Locale.US, "%.1f", displayWeight),
            dateInput = EventDateUtils.splitToAppDateTime(log.loggedAt).first.ifBlank { log.loggedAt.take(10) },
            error = null
        )
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(
            editingLogId = null,
            weightInput = "",
            dateInput = "",
            error = null
        )
    }

    fun save() {
        val s = _uiState.value
        val weight = s.weightInput.toDoubleOrNull()
        val weightKg = weight?.let { if (s.preferredUnit == "lb") it / 2.20462 else it }
        val loggedAt = toIso(s.dateInput)

        val validationError = when {
            weightKg == null || weightKg <= 0.0 -> "Weight must be greater than zero."
            s.dateInput.isBlank() || loggedAt == null -> "Date is required."
            isBeforeBirth(s.dateInput, s.petBirthDateIso) ->
                "Weight log date cannot be before pet's birth date (${s.petBirthDateIso?.take(10)})."
            else -> null
        }
        if (validationError != null) {
            _uiState.value = s.copy(error = validationError)
            return
        }

        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true, error = null)
            val result = if (s.editingLogId == null) {
                FeatureExecutionTracker.track("Create Weight Log") {
                    RepositoryProvider.weightLogRepository.createWeightLog(
                        s.petId,
                        CreateWeightLogRequest(
                            weight = weightKg!!,
                            loggedAt = loggedAt!!,
                            clientMutationId = UUID.randomUUID().toString()
                        )
                    )
                }
            } else {
                FeatureExecutionTracker.track("Edit Weight Log") {
                    RepositoryProvider.weightLogRepository.updateWeightLog(
                        s.petId,
                        s.editingLogId,
                        UpdateWeightLogRequest(weight = weightKg, loggedAt = loggedAt)
                    )
                }
            }

            result.fold(
                onSuccess = {
                    val logs = refreshLogsAndSyncCurrentWeight(s.petId)
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        editingLogId = null,
                        weightInput = "",
                        dateInput = "",
                        logs = logs,
                        selectedLogId = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save weight log"
                    )
                }
            )
        }
    }

    fun delete(log: WeightLog) {
        val petId = _uiState.value.petId
        viewModelScope.launch {
            FeatureExecutionTracker.track("Delete Weight Log") {
                RepositoryProvider.weightLogRepository.deleteWeightLog(petId, log.id)
            }.fold(
                onSuccess = {
                    val logs = refreshLogsAndSyncCurrentWeight(petId)
                    _uiState.value = _uiState.value.copy(
                        logs = logs,
                        selectedLogId = null
                    )
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    private fun toIso(raw: String): String? =
        EventDateUtils.toIsoFromAppDateTime(raw, "12:00 AM")

    private fun isBeforeBirth(rawDate: String, birthIso: String?): Boolean {
        val selected = parseLocalDate(rawDate) ?: return false
        val birth = birthIso?.let { parseLocalDate(it) } ?: return false
        return selected.isBefore(birth)
    }

    private fun parseLocalDate(raw: String): LocalDate? = runCatching {
        when {
            raw.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> {
                val p = raw.split("/")
                LocalDate.of(p[2].toInt(), p[1].toInt(), p[0].toInt())
            }
            raw.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) -> LocalDate.parse(raw.take(10))
            else -> null
        }
    }.getOrNull()

    private suspend fun refreshLogsAndSyncCurrentWeight(petId: String): List<WeightLog> {
        val logs = RepositoryProvider.weightLogRepository.getWeightLogs(petId).getOrDefault(emptyList())
        latestWeightLog(logs)?.let { latest ->
            RepositoryProvider.petRepository.updatePet(
                petId,
                UpdatePetRequest(weight = latest.weight)
            )
        }
        return logs
    }
}
