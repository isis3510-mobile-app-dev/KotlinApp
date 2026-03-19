package com.example.petcare.ui.screens.addPetForm

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.CreatePetRequest
import com.example.petcare.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
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
    val gender: String = "",
    val weight: String = "",   // kept as String so the text field can hold partial input
    val color: String  = "",
    // Step 3 – Medical (all optional)
    val defaultVet: String     = "",
    val defaultClinic: String  = "",
    val knownAllergies: String = "",
    // UI
    val isLoading: Boolean = false,
    val error: String?     = null
)

class AddPetViewModel(application: Application) : AndroidViewModel(application)  {
    private fun getContext(): Context = getApplication<Application>().applicationContext

    private val _state = MutableStateFlow(AddPetFormState())
    val state: StateFlow<AddPetFormState> = _state.asStateFlow()

    // ── Setters (one per field) ───────────────────────────────────────────────
    fun setPhotoUri(uri: Uri?) {
        val persisted = uri?.let { persistUri(it) }
        _state.value = _state.value.copy(photoUri = persisted)
    }
    fun clearPhoto()           { _state.value = _state.value.copy(photoUri = null) }
    fun setName(v: String)           { _state.value = _state.value.copy(name = v) }
    fun setBreed(v: String)          { _state.value = _state.value.copy(breed = v) }
    fun setSpecies(v: String)        { _state.value = _state.value.copy(species = v) }
    fun setGender(v: String)         { _state.value = _state.value.copy(gender = v) }
    fun setWeight(v: String)         { _state.value = _state.value.copy(weight = v) }
    fun setColor(v: String)          { _state.value = _state.value.copy(color = v) }
    fun setDefaultVet(v: String)     { _state.value = _state.value.copy(defaultVet = v) }
    fun setDefaultClinic(v: String)  { _state.value = _state.value.copy(defaultClinic = v) }
    fun setKnownAllergies(v: String) { _state.value = _state.value.copy(knownAllergies = v) }


    // ── Submit ────────────────────────────────────────────────────────────────
    /**
     * Calls POST /api/pets/ and invokes [onSuccess] with the new petId on success.
     * Any error is written to state.error.
     */
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

    fun submit(onSuccess: (petId: String) -> Unit) {
        val s = _state.value
        if (s.name.isBlank() || s.species.isBlank()) {
            _state.value = s.copy(error = "Name and species are required")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true, error = null)
            val photoUrl = s.photoUri?.let { uploadPhoto(it) }

            val request = CreatePetRequest(
                name           = s.name.trim(),
                species        = s.species.trim().lowercase(),
                breed          = s.breed.trim(),
                gender         = s.gender.trim().lowercase(),
                weight         = s.weight.toDoubleOrNull(),
                color          = s.color.trim(),
                knownAllergies = s.knownAllergies.trim(),
                defaultVet     = s.defaultVet.trim(),
                defaultClinic  = s.defaultClinic.trim(),
                photoUrl       = photoUrl
            )
            RepositoryProvider.petRepository.createPet(request).fold(
                onSuccess = { pet ->
                    _state.value = _state.value.copy(isLoading = false)
                    onSuccess(pet.id)
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

    fun clearError() { _state.value = _state.value.copy(error = null) }
    /** Call after successful navigation so the form is blank for the next pet. */
    fun reset()      { _state.value = AddPetFormState() }

    // En AddPetViewModel
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

}