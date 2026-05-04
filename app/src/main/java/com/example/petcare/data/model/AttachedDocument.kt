package com.example.petcare.data.model

import com.google.gson.annotations.SerializedName

data class AttachedDocument(
    @SerializedName(value = "id", alternate = ["_id", "documentId"])
    val id: String? = null,
    @SerializedName(value = "fileName", alternate = ["file_name"])
    val fileName: String,
    @SerializedName(value = "fileUri", alternate = ["file_uri", "url"])
    val fileUri: String? = null
)
