package com.example.petcare.ui.screens.petprofile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.UpdatePetRequest
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.normalizeForCommit
import com.example.petcare.util.sanitizeForEditing
import com.example.petcare.util.trimToNullIfBlank
import com.example.petcare.util.validateCommittedInput
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class EditPetState(
    val name: String           = "",
    val breed: String          = "",
    val weight: String         = "",
    val color: String          = "",
    val birthDate: String      = "",   // dd/MM/yyyy from DateTextField, or yyyy-MM-dd from API
    val knownAllergies: String = "",
    val defaultVet: String     = "",
    val defaultClinic: String  = "",
    val photoUrl: String?      = null,
    val pendingPhotoUri: Uri?  = null,
    val isSaving: Boolean      = false,
    val isSaved: Boolean       = false,
    val error: String?         = null
)

class EditPetViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(EditPetState())
    val state: StateFlow<EditPetState> = _state.asStateFlow()

    fun init(
        name: String,
        breed: String,
        weight: String,
        color: String,
        birthDate: String,
        knownAllergies: String,
        defaultVet: String,
        defaultClinic: String,
        photoUrl: String?
    ) {
        if (_state.value.name.trim().isNotEmpty()) return
        _state.value = EditPetState(
            name           = sanitizeForEditing(name, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PET_NAME).value,
            breed          = sanitizeForEditing(breed, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.BREED).value,
            weight         = sanitizeForEditing(weight, InputFieldPolicy.DECIMAL, InputTextLimits.WEIGHT, 199.0).value,
            color          = sanitizeForEditing(color, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.COLOR).value,
            birthDate      = birthDate,
            knownAllergies = sanitizeForEditing(knownAllergies, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.NOTES).value,
            defaultVet     = sanitizeForEditing(defaultVet, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value,
            defaultClinic  = sanitizeForEditing(defaultClinic, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value,
            photoUrl       = photoUrl
        )
    }

    fun setName(v: String)           { _state.value = _state.value.copy(name = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PET_NAME).value) }
    fun setBreed(v: String)          { _state.value = _state.value.copy(breed = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.BREED).value) }
    fun setWeight(v: String)         { _state.value = _state.value.copy(weight = sanitizeForEditing(v, InputFieldPolicy.DECIMAL, InputTextLimits.WEIGHT, 199.0).value) }
    fun setColor(v: String)          { _state.value = _state.value.copy(color = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.COLOR).value) }
    fun setBirthDate(v: String)      { _state.value = _state.value.copy(birthDate = v) }
    fun setKnownAllergies(v: String) { _state.value = _state.value.copy(knownAllergies = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.NOTES).value) }
    fun setDefaultVet(v: String)     { _state.value = _state.value.copy(defaultVet = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value) }
    fun setDefaultClinic(v: String)  { _state.value = _state.value.copy(defaultClinic = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value) }
    fun setPendingPhotoUri(uri: Uri?) { _state.value = _state.value.copy(pendingPhotoUri = uri) }
    fun clearError()                 { _state.value = _state.value.copy(error = null) }
    fun resetSaved()                 { _state.value = _state.value.copy(isSaved = false) }

    fun save(petId: String) {
        val s = _state.value
        val validationMessage = listOfNotNull(
            validateCommittedInput(s.name, InputFieldPolicy.GENERAL_TEXT, required = true, maxLength = InputTextLimits.PET_NAME, fieldName = "Name"),
            validateCommittedInput(s.breed, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.BREED, fieldName = "Breed"),
            validateCommittedInput(s.weight, InputFieldPolicy.DECIMAL, maxLength = InputTextLimits.WEIGHT, maxNumericValue = 199.0, fieldName = "Weight"),
            validateCommittedInput(s.color, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.COLOR, fieldName = "Color"),
            validateCommittedInput(s.knownAllergies, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.NOTES, fieldName = "Known allergies"),
            validateCommittedInput(s.defaultVet, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.PROVIDER_OR_CLINIC, fieldName = "Veterinarian"),
            validateCommittedInput(s.defaultClinic, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.PROVIDER_OR_CLINIC, fieldName = "Clinic")
        ).firstOrNull()
        if (validationMessage != null) {
            _state.value = s.copy(error = validationMessage)
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, error = null)

            val finalPhotoUrl: String? = if (s.pendingPhotoUri != null) {
                uploadPhoto(s.pendingPhotoUri)
            } else {
                s.photoUrl
            }

            val request = UpdatePetRequest(
                name           = normalizeForCommit(s.name, InputFieldPolicy.GENERAL_TEXT).trimToNullIfBlank(),
                breed          = normalizeForCommit(s.breed, InputFieldPolicy.GENERAL_TEXT).trimToNullIfBlank(),
                weight         = normalizeForCommit(s.weight, InputFieldPolicy.DECIMAL).trimToNullIfBlank()?.toDoubleOrNull(),
                color          = normalizeForCommit(s.color, InputFieldPolicy.GENERAL_TEXT).trimToNullIfBlank(),
                birthDate      = s.birthDate.takeIf { it.isNotBlank() }?.let { toIso(it) },
                knownAllergies = normalizeForCommit(s.knownAllergies, InputFieldPolicy.GENERAL_TEXT),
                defaultVet     = normalizeForCommit(s.defaultVet, InputFieldPolicy.GENERAL_TEXT),
                defaultClinic  = normalizeForCommit(s.defaultClinic, InputFieldPolicy.GENERAL_TEXT),
                photoUrl       = finalPhotoUrl
            )

            FeatureExecutionTracker.track("Edit Pet") {
                RepositoryProvider.petRepository.updatePet(petId, request)
            }.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isSaving = false, isSaved = true)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error    = e.message ?: "Failed to save changes"
                    )
                }
            )
        }
    }

    private suspend fun uploadPhoto(uri: Uri): String? {
        return try {
            val filename = "pets/${UUID.randomUUID()}.jpg"
            val ref = Firebase.storage.reference.child(filename)
            ref.putFile(uri).await()
            val bucket      = ref.bucket
            val encodedPath = filename.replace("/", "%2F")
            "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedPath?alt=media"
        } catch (e: Exception) {
            android.util.Log.e("EditPetViewModel", "Photo upload failed: ${e.message}")
            null
        }
    }

    /**
     * Accepts both dd/MM/yyyy (from the DateTextField picker) and
     * yyyy-MM-dd (already stored in the backend) and normalises to ISO-8601.
     */
    private fun toIso(date: String): String = try {
        when {
            date.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> {
                val p = date.split("/")
                "${p[2]}-${p[1]}-${p[0]}T00:00:00Z"
            }
            date.matches(Regex("""\d{4}-\d{2}-\d{2}.*""")) -> {
                "${date.take(10)}T00:00:00Z"
            }
            else -> date
        }
    } catch (_: Exception) { date }
}
