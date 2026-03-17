package com.example.petcare.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ── Data model returned by both read and write flows ─────────────────────────


// ─────────────────────────────────────────────────────────────────────────────

class NfcRepositoryMock(
    /** Override in tests or to point at a staging server. */
    private val baseUrl: String = DEFAULT_BASE_URL
) {

    companion object {
        // 10.0.2.2 → localhost from the Android emulator.
        // Replace with your real server URL for device testing / production.
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/api"
        private const val TIMEOUT_MS = 8_000
    }

    // ── Write flow ────────────────────────────────────────────────────────────

    /**
     * Fetches the data bundle to write onto the NFC tag.
     * Requires a valid Firebase ID token.
     *
     * Endpoint: GET /api/pets/<petId>/nfc-payload/
     */
    /**
    suspend fun fetchWritePayload(
        petId: String,
        firebaseToken: String
    ): Result<NfcPetPayload> = withContext(Dispatchers.IO) {
        get(
            url   = "$baseUrl/pets/$petId/nfc-payload/",
            token = firebaseToken
        ) { json ->
            NfcPetPayload(
                petId        = json.optString("petId"),
                petName      = json.optString("petName"),
                species      = json.optString("species"),
                breed        = json.optString("breed"),
                ownerName    = json.optString("ownerName"),
                ownerPhone   = json.optString("ownerPhone"),
                appDeepLink  = json.optString("appDeepLink"),
            )
        }
    }*/
    /**
     * MOCK VERSION: Fetches a fake data bundle to test NFC writing.
     */
    suspend fun fetchWritePayload(
        petId: String,
        firebaseToken: String
    ): Result<NfcPetPayload> = withContext(Dispatchers.IO) {
        // Simulate a small network delay
        kotlinx.coroutines.delay(500)

        // Return a hardcoded success object
        Result.success(
            NfcPetPayload(
                petId = petId,
                petName = "Mocky the Dog",
                species = "Dog",
                breed = "Golden Retriever",
                ownerName = "Test Owner",
                ownerPhone = "+123456789",
                appDeepLink = "petcare://pet/$petId" // This is what actually gets written to the tag
            )
        )
    }

    /**
     * Marks the pet as NFC-synced in the backend after a successful tag write.
     *
     * Endpoint: POST /api/pets/<petId>/nfc-sync/
     */
    /**
    suspend fun markNfcSynced(
        petId: String,
        firebaseToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection("$baseUrl/pets/$petId/nfc-sync/", "POST", firebaseToken)
            val code = conn.responseCode
            conn.disconnect()
            if (code == 200) Result.success(Unit)
            else Result.failure(Exception("NFC sync failed — server returned $code"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/
    suspend fun markNfcSynced(
        petId: String,
        firebaseToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    // ── Read flow ─────────────────────────────────────────────────────────────

    /**
     * Fetches pet + owner info after scanning a tag.
     * No authentication required — anyone can call this.
     *
     * Endpoint: GET /api/nfc/read/<petId>/
     */
    /**suspend fun fetchPublicPetInfo(petId: String): Result<NfcPetPayload> =
        withContext(Dispatchers.IO) {
            get(url = "$baseUrl/nfc/read/$petId/") { json ->
                NfcPetPayload(
                    petId         = json.optString("petId"),
                    petName       = json.optString("petName"),
                    species       = json.optString("species"),
                    breed         = json.optString("breed"),
                    ownerName     = json.optString("ownerName"),
                    ownerPhone    = json.optString("ownerPhone"),
                    ownerInitials = json.optString("ownerInitials"),
                    photoUrl      = json.optString("photoUrl"),
                    status        = json.optString("status", "Unknown"),
                )
            }
        }*/
    suspend fun fetchPublicPetInfo(petId: String): Result<NfcPetPayload> =
        withContext(Dispatchers.IO) {
            kotlinx.coroutines.delay(500)
            Result.success(
                NfcPetPayload(
                    petId = petId,
                    petName = "Scanned Buddy",
                    species = "Cat",
                    breed = "Siamese",
                    ownerName = "John Doe",
                    ownerPhone = "555-0199",
                    ownerInitials = "JD",
                    status = "Found"
                )
            )
        }

    // ── Private HTTP helpers ──────────────────────────────────────────────────

    /** GET helper — parses the response JSON and maps it with [transform]. */
    private fun <T> get(
        url: String,
        token: String? = null,
        transform: (JSONObject) -> T
    ): Result<T> = try {
        val conn = openConnection(url, "GET", token)
        val code = conn.responseCode
        if (code != 200) {
            conn.disconnect()
            Result.failure(Exception("HTTP $code from $url"))
        } else {
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            Result.success(transform(JSONObject(body)))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun openConnection(
        url: String,
        method: String,
        token: String? = null
    ): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod  = method
            connectTimeout = TIMEOUT_MS
            readTimeout    = TIMEOUT_MS
            setRequestProperty("Content-Type", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
        }
}