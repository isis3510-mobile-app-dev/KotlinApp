package com.example.petcare.data.model

import android.net.Uri

/**
 * Representa un documento seleccionado por el usuario que está siendo
 * (o ya fue) subido a Firebase Storage antes de que exista el ID
 * del recurso padre (eventId o vaccinationId).
 */
data class StagedDocument(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    // null mientras está subiendo, se llena cuando Firebase confirma el upload
    val downloadUrl: String? = null,
    val isUploading: Boolean = false,
    val error: String? = null
)