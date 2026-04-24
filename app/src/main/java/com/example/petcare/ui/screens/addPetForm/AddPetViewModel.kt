package com.example.petcare.ui.screens.addPetForm

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.analytics.FeatureExecutionTracker
import com.example.petcare.data.model.CreatePetRequest
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.example.petcare.data.model.Pet
import com.example.petcare.data.network.isOnline
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.enforceMaxLength
import com.example.petcare.util.normalizeForCommit
import com.example.petcare.util.sanitizeForEditing
import com.example.petcare.util.trimToNullIfBlank
import com.example.petcare.util.validateCommittedInput
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

data class AddPetFormState(
    // Step 1 – Basic Info
    val photoUri: Uri? = null,
    val name: String    = "",
    val breed: String   = "",
    val species: String = "",
    // Step 2 – Details
    val gender: String    = "",
    val weight: String    = "",
    val color: String     = "",
    val birthDate: String = "",   // NEW: dd/MM/yyyy from DateTextField
    // Step 3 – Medical (all optional)
    val defaultVet: String     = "",
    val defaultClinic: String  = "",
    val knownAllergies: String = "",
    // UI
    val isLoading: Boolean = false,
    val error: String?     = null
)

class AddPetViewModel(application: Application) : AndroidViewModel(application) {
    private fun getContext(): Context = getApplication<Application>().applicationContext

    private val _state = MutableStateFlow(AddPetFormState())
    val state: StateFlow<AddPetFormState> = _state.asStateFlow()

    fun setPhotoUri(uri: Uri?) {
        val persisted = uri?.let { persistUri(it) }
        _state.value = _state.value.copy(photoUri = persisted)
    }
    fun clearPhoto()               { _state.value = _state.value.copy(photoUri = null) }
    fun setName(v: String)         { _state.value = _state.value.copy(name = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PET_NAME).value) }
    fun setBreed(v: String)        { _state.value = _state.value.copy(breed = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.BREED).value) }
    fun setSpecies(v: String)      { _state.value = _state.value.copy(species = normalizeForCommit(v, InputFieldPolicy.GENERAL_TEXT)) }
    fun setGender(v: String)       { _state.value = _state.value.copy(gender = normalizeForCommit(v, InputFieldPolicy.GENERAL_TEXT), error = null) }
    fun setWeight(v: String)       { _state.value = _state.value.copy(weight = sanitizeForEditing(v, InputFieldPolicy.DECIMAL, InputTextLimits.WEIGHT, 199.0).value) }
    fun setColor(v: String)        { _state.value = _state.value.copy(color = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.COLOR).value) }
    fun setBirthDate(v: String)    { _state.value = _state.value.copy(birthDate = v) }   // NEW
    fun setDefaultVet(v: String)     { _state.value = _state.value.copy(defaultVet = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value) }
    fun setDefaultClinic(v: String)  { _state.value = _state.value.copy(defaultClinic = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.PROVIDER_OR_CLINIC).value) }
    fun setKnownAllergies(v: String) { _state.value = _state.value.copy(knownAllergies = sanitizeForEditing(v, InputFieldPolicy.GENERAL_TEXT, InputTextLimits.NOTES).value) }

    private suspend fun uploadPhoto(uri: Uri): String? {
        return try {
            val filename = "pets/${UUID.randomUUID()}.jpg"
            val ref = Firebase.storage.reference.child(filename)
            ref.putFile(uri).await()
            val bucket = ref.bucket
            val encodedPath = filename.replace("/", "%2F")
            "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedPath?alt=media"
        } catch (e: Exception) {
            null
        }
    }

    fun submit(onSuccess: (pet: Pet) -> Unit) {
        val s = _state.value
        val nameError = validateCommittedInput(s.name, InputFieldPolicy.GENERAL_TEXT, required = true, maxLength = InputTextLimits.PET_NAME, fieldName = "Name")
        val speciesError = if (normalizeForCommit(s.species, InputFieldPolicy.GENERAL_TEXT).isBlank()) "Species cannot be blank." else null
        val genderError = if (normalizeForCommit(s.gender, InputFieldPolicy.GENERAL_TEXT).isBlank()) "Gender cannot be blank." else null
        val weightError = validateCommittedInput(s.weight, InputFieldPolicy.DECIMAL, maxLength = InputTextLimits.WEIGHT, maxNumericValue = 199.0, fieldName = "Weight")
        val breedError = validateCommittedInput(s.breed, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.BREED)
        val colorError = validateCommittedInput(s.color, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.COLOR)
        val vetError = validateCommittedInput(s.defaultVet, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.PROVIDER_OR_CLINIC)
        val clinicError = validateCommittedInput(s.defaultClinic, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.PROVIDER_OR_CLINIC)
        val allergiesError = validateCommittedInput(s.knownAllergies, InputFieldPolicy.GENERAL_TEXT, maxLength = InputTextLimits.NOTES)
        val birthDateError = if (s.birthDate.isBlank()) "Date of Birth is required." else null

        val firstError = listOfNotNull(
            nameError,
            speciesError,
            genderError,
            birthDateError,
            weightError,
            breedError,
            colorError,
            vetError,
            clinicError,
            allergiesError
        ).firstOrNull()

        if (firstError != null) {
            _state.value = s.copy(error = firstError)
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)
            val photoUrl: String? = if (isOnline(getContext())) {
                s.photoUri?.let { uploadPhoto(it) }
            } else {
                s.photoUri?.toString()
            }

            val request = CreatePetRequest(
                name           = normalizeForCommit(s.name, InputFieldPolicy.GENERAL_TEXT),
                species        = normalizeForCommit(s.species, InputFieldPolicy.GENERAL_TEXT).lowercase(),
                breed          = normalizeForCommit(s.breed, InputFieldPolicy.GENERAL_TEXT),
                gender         = normalizeForCommit(s.gender, InputFieldPolicy.GENERAL_TEXT).lowercase(),
                weight         = normalizeForCommit(s.weight, InputFieldPolicy.DECIMAL).trimToNullIfBlank()?.toDoubleOrNull(),
                color          = normalizeForCommit(s.color, InputFieldPolicy.GENERAL_TEXT),
                birthDate      = s.birthDate.takeIf { it.isNotBlank() }?.let { toIso(it) },
                knownAllergies = normalizeForCommit(s.knownAllergies, InputFieldPolicy.GENERAL_TEXT),
                defaultVet     = normalizeForCommit(s.defaultVet, InputFieldPolicy.GENERAL_TEXT),
                defaultClinic  = normalizeForCommit(s.defaultClinic, InputFieldPolicy.GENERAL_TEXT),
                photoUrl       = photoUrl
            )
            FeatureExecutionTracker.track("Create Pet") {
                RepositoryProvider.petRepository.createPet(request)
            }.fold(
                onSuccess = { pet ->
                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess(pet)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error     = e.message ?: "Unknown error"
                    )
                }
            )
        }
    }

    fun setError(message: String) { _state.value = _state.value.copy(error = message) }
    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun reset()      { _state.value = AddPetFormState() }

    private fun persistUri(uri: Uri): Uri {
        val context = getApplication<Application>().applicationContext
        val filename = "pet_${System.currentTimeMillis()}.jpg"
        val dest = File(context.filesDir, "pets/$filename")
            .also { it.parentFile?.mkdirs() }
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        return Uri.fromFile(dest)
    }

    /** Converts dd/MM/yyyy → yyyy-MM-ddT00:00:00Z */
    private fun toIso(date: String): String = try {
        val p = date.split("/")
        if (p.size == 3) "${p[2]}-${p[1]}-${p[0]}T00:00:00Z" else date
    } catch (_: Exception) { date }
}
