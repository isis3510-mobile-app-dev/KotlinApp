package com.example.petcare.ui.screens.petprofile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.data.model.UpdatePetRequest
import com.example.petcare.data.repository.RepositoryProvider
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
        if (_state.value.name.isNotBlank()) return
        _state.value = EditPetState(
            name           = name,
            breed          = breed,
            weight         = weight,
            color          = color,
            birthDate      = birthDate,
            knownAllergies = knownAllergies,
            defaultVet     = defaultVet,
            defaultClinic  = defaultClinic,
            photoUrl       = photoUrl
        )
    }

    fun setName(v: String)           { _state.value = _state.value.copy(name = v) }
    fun setBreed(v: String)          { _state.value = _state.value.copy(breed = v) }
    fun setWeight(v: String)         { _state.value = _state.value.copy(weight = v) }
    fun setColor(v: String)          { _state.value = _state.value.copy(color = v) }
    fun setBirthDate(v: String)      { _state.value = _state.value.copy(birthDate = v) }
    fun setKnownAllergies(v: String) { _state.value = _state.value.copy(knownAllergies = v) }
    fun setDefaultVet(v: String)     { _state.value = _state.value.copy(defaultVet = v) }
    fun setDefaultClinic(v: String)  { _state.value = _state.value.copy(defaultClinic = v) }
    fun setPendingPhotoUri(uri: Uri?) { _state.value = _state.value.copy(pendingPhotoUri = uri) }
    fun clearError()                 { _state.value = _state.value.copy(error = null) }
    fun resetSaved()                 { _state.value = _state.value.copy(isSaved = false) }

    fun save(petId: String) {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Name is required")
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
                name           = s.name.trim().takeIf { it.isNotBlank() },
                breed          = s.breed.trim().takeIf { it.isNotBlank() },
                weight         = s.weight.toDoubleOrNull(),
                color          = s.color.trim().takeIf { it.isNotBlank() },
                birthDate      = s.birthDate.takeIf { it.isNotBlank() }?.let { toIso(it) },
                knownAllergies = s.knownAllergies.trim(),
                defaultVet     = s.defaultVet.trim(),
                defaultClinic  = s.defaultClinic.trim(),
                photoUrl       = finalPhotoUrl
            )

            RepositoryProvider.petRepository.updatePet(petId, request).fold(
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