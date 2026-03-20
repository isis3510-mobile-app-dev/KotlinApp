package com.example.petcare.util

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseStorageHelper {

    private val storage = Firebase.storage

    suspend fun uploadPetPhoto(uri: Uri): String {
        val ref = storage.reference.child("pets/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}