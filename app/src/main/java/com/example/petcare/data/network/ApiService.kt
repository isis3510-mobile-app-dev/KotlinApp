package com.example.petcare.data.network
import com.example.petcare.data.model.UpdateUserRequest
import com.example.petcare.data.model.User
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("users/me/")
    suspend fun getMe(): Response<User>

    @PATCH("users/me/")
    suspend fun updateMe(@Body body: UpdateUserRequest): Response<User>

    @DELETE("users/me/")
    suspend fun deleteMe(): Response<Unit>

}