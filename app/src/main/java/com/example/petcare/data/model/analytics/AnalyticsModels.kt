package com.example.petcare.data.model.analytics

import com.google.gson.annotations.SerializedName

// ─── Screen & Button DTOs ─────────────────────────────────────────────────────

data class ButtonDto(
    @SerializedName("buttonId") val buttonId: String,
    @SerializedName("schema")   val schema: Int = 1,
    @SerializedName("name")     val name: String
)

data class ScreenDto(
    @SerializedName("id")      val id: String? = null,
    @SerializedName("schema")  val schema: Int = 1,
    @SerializedName("name")    val name: String,
    @SerializedName("hasAds")  val hasAds: Boolean = false,
    @SerializedName("appType") val appType: String = "Kotlin",
    @SerializedName("buttons") val buttons: List<ButtonDto> = emptyList()
)

// ─── Feature DTO ──────────────────────────────────────────────────────────────

data class FeatureDto(
    @SerializedName("id")           val id: String? = null,
    @SerializedName("schema")       val schema: Int = 1,
    @SerializedName("name")         val name: String,
    @SerializedName("originButton") val originButton: String,
    @SerializedName("originScreen") val originScreen: String,
    @SerializedName("appType")      val appType: String = "Kotlin"
)

// ─── Feature Route DTO ────────────────────────────────────────────────────────

data class FeatureRouteDto(
    @SerializedName("id")           val id: String? = null,
    @SerializedName("schema")       val schema: Int = 1,
    @SerializedName("name")         val name: String,
    @SerializedName("originButton") val originButton: String,
    @SerializedName("originScreen") val originScreen: String,
    @SerializedName("endButton")    val endButton: String,
    @SerializedName("endScreen")    val endScreen: String,
    @SerializedName("appType")      val appType: String = "Kotlin"
)

// ─── Log Request Bodies ───────────────────────────────────────────────────────

data class CreateScreenTimeLogRequest(
    @SerializedName("schema")   val schema: Int = 1,
    @SerializedName("userId")   val userId: String,
    @SerializedName("screenId") val screenId: String,
    @SerializedName("startTime") val startTime: String,   // ISO 8601
    @SerializedName("endTime")   val endTime: String,     // ISO 8601
    @SerializedName("totalTime") val totalTime: Int,      // seconds
    @SerializedName("appType")  val appType: String = "Kotlin"
)

data class CreateFeatureExecutionLogRequest(
    @SerializedName("schema")        val schema: Int = 1,
    @SerializedName("userId")        val userId: String,
    @SerializedName("featureId")     val featureId: String,
    @SerializedName("startTime")     val startTime: String,   // ISO 8601
    @SerializedName("endTime")       val endTime: String,     // ISO 8601
    @SerializedName("totalTime")     val totalTime: Int,      // seconds
    @SerializedName("downloadSpeed") val downloadSpeed: Int,  // kbps
    @SerializedName("uploadSpeed")   val uploadSpeed: Int,    // kbps
    @SerializedName("appType")       val appType: String = "Kotlin"
)

data class CreateFeatureClicksLogRequest(
    @SerializedName("schema")    val schema: Int = 1,
    @SerializedName("userId")    val userId: String,
    @SerializedName("routeId")   val routeId: String,
    @SerializedName("timestamp") val timestamp: String,   // ISO 8601
    @SerializedName("nClicks")   val nClicks: Int,
    @SerializedName("appType")   val appType: String = "Kotlin"
)
