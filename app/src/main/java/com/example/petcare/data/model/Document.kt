package com.example.petcare.data.model

import com.google.gson.annotations.SerializedName

data class AddDocumentRequest(
    @SerializedName("documentId") val documentId: String? = null,
    @SerializedName("fileName")   val fileName: String,
    @SerializedName("fileUri")    val fileUri: String? = null
)
