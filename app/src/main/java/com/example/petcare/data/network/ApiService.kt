package com.example.petcare.data.network

import com.example.petcare.data.model.AddDocumentRequest
import com.example.petcare.data.model.AddVaccinationRequest
import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.CreateNotificationRequest
import com.example.petcare.data.model.CreatePetRequest
import com.example.petcare.data.model.Event
import com.example.petcare.data.model.NfcPayloadResponse
import com.example.petcare.data.model.NfcPublicReadResponse
import com.example.petcare.data.model.NfcSyncResponse
import com.example.petcare.data.model.AppNotification
import com.example.petcare.data.model.Pet
import com.example.petcare.data.model.PetSmartResponse
import com.example.petcare.data.model.UpdateNotificationRequest
import com.example.petcare.data.model.UpdatePetRequest
import com.example.petcare.data.model.UpdateUserRequest
import com.example.petcare.data.model.UpdateVaccinationRequest
import com.example.petcare.data.model.User
import com.example.petcare.data.model.Vaccine
import com.example.petcare.data.model.analytics.CreateFeatureClicksLogRequest
import com.example.petcare.data.model.analytics.CreateFeatureExecutionLogRequest
import com.example.petcare.data.model.analytics.CreateScreenTimeLogRequest
import com.example.petcare.data.model.analytics.FeatureDto
import com.example.petcare.data.model.analytics.FeatureRouteDto
import com.example.petcare.data.model.analytics.ScreenDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ── Users ─────────────────────────────────────────────────────────────

    @GET("users/me/")
    suspend fun getMe(): Response<User>

    @PATCH("users/me/")
    suspend fun updateMe(@Body body: UpdateUserRequest): Response<User>

    @DELETE("users/me/")
    suspend fun deleteMe(): Response<Unit>

    // ── Pets ──────────────────────────────────────────────────────────────

    @GET("pets/mine/")
    suspend fun getPets(): Response<List<Pet>>

    @GET("pets/{petId}/")
    suspend fun getPet(@Path("petId") petId: String): Response<Pet>

    @POST("pets/")
    suspend fun createPet(@Body body: CreatePetRequest): Response<Pet>

    @PUT("pets/{petId}/")
    suspend fun updatePet(
        @Path("petId") petId: String,
        @Body body: UpdatePetRequest
    ): Response<Pet>

    @DELETE("pets/{petId}/")
    suspend fun deletePet(@Path("petId") petId: String): Response<Unit>

    // ── Vaccinations (embedded in Pet) ────────────────────────────────────

    @POST("pets/{petId}/vaccinations/")
    suspend fun addVaccination(
        @Path("petId") petId: String,
        @Body body: AddVaccinationRequest
    ): Response<Pet>

    @PUT("pets/{petId}/vaccinations/{vaccinationId}/")
    suspend fun updateVaccination(
        @Path("petId") petId: String,
        @Path("vaccinationId") vaccinationId: String,
        @Body body: UpdateVaccinationRequest
    ): Response<Pet>

    // DELETE with body — Retrofit needs @HTTP for this
    @DELETE("pets/{petId}/vaccinations/{vaccinationId}/")
    suspend fun deleteVaccination(
        @Path("petId") petId: String,
        @Path("vaccinationId") vaccinationId: String
    ): Response<Pet>

    @GET("vaccines/")
    suspend fun getVaccines(): Response<List<Vaccine>>

    // ── Vaccination Documents ─────────────────────────────────────────────

    @POST("pets/{petId}/vaccinations/{vaccinationId}/documents/")
    suspend fun addVaccinationDocument(
        @Path("petId") petId: String,
        @Path("vaccinationId") vaccinationId: String,
        @Body body: AddDocumentRequest
    ): Response<Pet>

    // ── Events ────────────────────────────────────────────────────────────

    @GET("events/")
    suspend fun getEvents(
        @Query("pet_id")   petId: String?   = null,
        @Query("owner_id") ownerId: String? = null
    ): Response<List<Event>>

    @POST("events/")
    suspend fun createEvent(@Body body: CreateEventRequest): Response<Event>

    @GET("events/{eventId}/")
    suspend fun getEvent(@Path("eventId") eventId: String): Response<Event>

    @PUT("events/{eventId}/")
    suspend fun updateEvent(
        @Path("eventId") eventId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Event>

    @DELETE("events/{eventId}/")
    suspend fun deleteEvent(@Path("eventId") eventId: String): Response<Unit>

    // ── Event Documents ───────────────────────────────────────────────────

    @POST("events/{eventId}/documents/")
    suspend fun addEventDocument(
        @Path("eventId") eventId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Event>

    // ── Notifications ────────────────────────────────────────────────────────

    @GET("notifications/")
    suspend fun getNotifications(
        @Query("user_id") userId: String? = null
    ): Response<List<AppNotification>>

    @POST("notifications/")
    suspend fun createNotification(
        @Body body: CreateNotificationRequest
    ): Response<AppNotification>

    @PUT("notifications/{notificationId}/")
    suspend fun updateNotification(
        @Path("notificationId") notificationId: String,
        @Body body: UpdateNotificationRequest
    ): Response<AppNotification>

    // ── NFC ───────────────────────────────────────────────────────────────

    @GET("pets/{petId}/nfc-payload/")
    suspend fun getNfcPayload(@Path("petId") petId: String): Response<NfcPayloadResponse>

    @POST("pets/{petId}/nfc-sync/")
    suspend fun syncNfc(@Path("petId") petId: String): Response<NfcSyncResponse>

    @GET("nfc/read/{petId}/")
    suspend fun nfcPublicRead(@Path("petId") petId: String): Response<NfcPublicReadResponse>

    @GET("pets/{petId}/smart/")
    suspend fun getPetSmart(
        @Path("petId") petId: String
    ): Response<PetSmartResponse>

    // ── Analytics — Screens ──────────────────────────────────────────────

    @GET("screens/")
    suspend fun getScreens(
        @Query("appType") appType: String = "Kotlin"
    ): Response<List<ScreenDto>>

    @POST("screens/")
    suspend fun createScreen(@Body body: ScreenDto): Response<ScreenDto>

    // ── Analytics — Features ─────────────────────────────────────────────

    @GET("features/")
    suspend fun getFeatures(
        @Query("appType") appType: String = "Kotlin"
    ): Response<List<FeatureDto>>

    @POST("features/")
    suspend fun createFeature(@Body body: FeatureDto): Response<FeatureDto>

    // ── Analytics — Feature Routes ───────────────────────────────────────

    @GET("feature-routes/")
    suspend fun getFeatureRoutes(
        @Query("appType") appType: String = "Kotlin"
    ): Response<List<FeatureRouteDto>>

    @POST("feature-routes/")
    suspend fun createFeatureRoute(@Body body: FeatureRouteDto): Response<FeatureRouteDto>

    // ── Analytics — Logs ─────────────────────────────────────────────────

    @POST("screen-time-logs/")
    suspend fun createScreenTimeLog(@Body body: CreateScreenTimeLogRequest): Response<Unit>

    @POST("feature-execution-logs/")
    suspend fun createFeatureExecutionLog(@Body body: CreateFeatureExecutionLogRequest): Response<Unit>

    @POST("feature-clicks-logs/")
    suspend fun createFeatureClicksLog(@Body body: CreateFeatureClicksLogRequest): Response<Unit>
}
