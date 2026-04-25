package com.example.petcare.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class UploadedDocument(
    val downloadUrl: String,
    val fileName: String,
    val mimeType: String
)

object FirebaseDocumentUploader {

    private val storage = Firebase.storage
    private const val TAG = "DOC_UPLOAD"

    /**
     * Ruta: pets/{petId}/documents/events/{eventId}/{uuid}_{filename}
     */
    suspend fun uploadEventDocument(
        context: Context,
        uri: Uri,
        petId: String,
        eventId: String
    ): Result<UploadedDocument> = runCatching {
        val fileName = getFileName(context, uri)
            ?: "document_${System.currentTimeMillis()}"
        val mimeType = context.contentResolver.getType(uri)
            ?: "application/octet-stream"
        val path = "pets/$petId/documents/events/$eventId/${UUID.randomUUID()}_$fileName"
        Log.d(TAG, "Event document upload requested petId=$petId eventId=$eventId path=$path")
        upload(uri, path, mimeType, fileName)
    }.onFailure { Log.e(TAG, "Event document upload failed petId=$petId eventId=$eventId: ${it.message}", it) }

    /**
     * Ruta: pets/{petId}/documents/vaccinations/{vaccinationId}/{uuid}_{filename}
     */
    suspend fun uploadVaccinationDocument(
        context: Context,
        uri: Uri,
        petId: String,
        vaccinationId: String
    ): Result<UploadedDocument> = runCatching {
        val fileName = getFileName(context, uri)
            ?: "document_${System.currentTimeMillis()}"
        val mimeType = context.contentResolver.getType(uri)
            ?: "application/octet-stream"
        val path = "pets/$petId/documents/vaccinations/$vaccinationId/${UUID.randomUUID()}_$fileName"
        Log.d(TAG, "Vaccination document upload requested petId=$petId vaccinationId=$vaccinationId path=$path")
        upload(uri, path, mimeType, fileName)
    }.onFailure { Log.e(TAG, "Vaccination document upload failed petId=$petId vaccinationId=$vaccinationId: ${it.message}", it) }

    private suspend fun upload(
        uri: Uri,
        path: String,
        mimeType: String,
        originalFileName: String
    ): UploadedDocument {
        val ref = storage.reference.child(path)
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()
        Log.d(
            TAG,
            "Firebase putFile start path=$path mimeType=$mimeType fileName=$originalFileName thread=${Thread.currentThread().name}"
        )
        ref.putFile(uri, metadata).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        Log.d(TAG, "Firebase putFile success path=$path downloadUrlReady=${downloadUrl.isNotBlank()}")
        return UploadedDocument(
            downloadUrl = downloadUrl,
            fileName    = originalFileName,
            mimeType    = mimeType
        )
    }

    fun getFileName(context: Context, uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                android.provider.OpenableColumns.DISPLAY_NAME
                            )
                        )
                    } else null
                }
            }
            "file" -> uri.lastPathSegment
            else   -> null
        }
    }

    suspend fun uploadEventDocumentStaging(
        context: Context,
        uri: Uri,
        petId: String,
        stagingId: String
    ): Result<UploadedDocument> = runCatching {
        val fileName = getFileName(context, uri)
            ?: "document_${System.currentTimeMillis()}"
        val mimeType = context.contentResolver.getType(uri)
            ?: "application/octet-stream"
        val path = "pets/$petId/documents/events/staging/$stagingId/${UUID.randomUUID()}_$fileName"
        Log.d(TAG, "Event staging upload requested petId=$petId stagingId=$stagingId path=$path")
        upload(uri, path, mimeType, fileName)
    }.onFailure { Log.e(TAG, "Event staging upload failed petId=$petId stagingId=$stagingId: ${it.message}", it) }

    /**
     * Sube a una carpeta de staging antes de conocer el vaccinationId real.
     * Ruta: pets/{petId}/documents/vaccinations/staging/{stagingId}/{uuid}_{filename}
     */
    suspend fun uploadVaccinationDocumentStaging(
        context: Context,
        uri: Uri,
        petId: String,
        stagingId: String
    ): Result<UploadedDocument> = runCatching {
        val fileName = getFileName(context, uri)
            ?: "document_${System.currentTimeMillis()}"
        val mimeType = context.contentResolver.getType(uri)
            ?: "application/octet-stream"
        val path = "pets/$petId/documents/vaccinations/staging/$stagingId/${UUID.randomUUID()}_$fileName"
        Log.d(TAG, "Vaccination staging upload requested petId=$petId stagingId=$stagingId path=$path")
        upload(uri, path, mimeType, fileName)
    }.onFailure { Log.e(TAG, "Vaccination staging upload failed petId=$petId stagingId=$stagingId: ${it.message}", it) }
}
