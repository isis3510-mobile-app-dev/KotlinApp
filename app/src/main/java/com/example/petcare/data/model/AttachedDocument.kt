package com.example.petcare.data.model

import com.google.gson.annotations.SerializedName

data class AttachedDocument(
    @SerializedName(value = "id", alternate = ["_id"])
    val id: String? = null,
    @SerializedName(value = "documentId", alternate = ["document_id", "docId", "doc_id"])
    val documentId: String? = null,
    @SerializedName(value = "fileName", alternate = ["file_name"])
    val fileName: String,
    @SerializedName(value = "fileUri", alternate = ["file_uri", "url"])
    val fileUri: String? = null
)
