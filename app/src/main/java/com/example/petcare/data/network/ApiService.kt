package com.example.petcare.data.network
import com.example.petcare.data.model.AddVaccinationRequest
import com.example.petcare.data.model.CreateEventRequest
import com.example.petcare.data.model.CreatePetRequest
import com.example.petcare.data.model.Event
import com.example.petcare.data.model.UpdateUserRequest
import com.example.petcare.data.model.User
import com.example.petcare.data.model.Pet
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.example.petcare.data.model.NfcPayloadResponse
import com.example.petcare.data.model.NfcPublicReadResponse
import com.example.petcare.data.model.NfcSyncResponse
import com.example.petcare.data.model.UpdatePetRequest
import retrofit2.http.Query

interface ApiService {
    @GET("users/me/")
    suspend fun getMe(): Response<User>

    @PATCH("users/me/")
    suspend fun updateMe(@Body body: UpdateUserRequest): Response<User>

    @DELETE("users/me/")
    suspend fun deleteMe(): Response<Unit>

    // Pets
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

    // Vaccines (embedded to pet)
    @POST("pets/{petId}/vaccinations/")
    suspend fun addVaccination(
        @Path("petId") petId: String,
        @Body body: AddVaccinationRequest
    ): Response<Pet>

    // Events

    @GET("events/")
    suspend fun getEvents(
        @Query("pet_id")   petId: String?   = null,
        @Query("owner_id") ownerId: String? = null
    ): Response<List<Event>>

    @POST("events/")
    suspend fun createEvent(@Body body: CreateEventRequest): Response<Event>

    @GET("events/{eventId}/")
    suspend fun getEvent(@Path("eventId") eventId: String): Response<Event>

    @DELETE("events/{eventId}/")
    suspend fun deleteEvent(@Path("eventId") eventId: String): Response<Unit>

    //  NFC
    @GET("pets/{petId}/nfc-payload/")
    suspend fun getNfcPayload(@Path("petId") petId: String): Response<NfcPayloadResponse>

    @POST("pets/{petId}/nfc-sync/")
    suspend fun syncNfc(@Path("petId") petId: String): Response<NfcSyncResponse>

    @GET("nfc/read/{petId}/")
    suspend fun nfcPublicRead(@Path("petId") petId: String): Response<NfcPublicReadResponse>

}