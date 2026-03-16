package com.example.petcare.data.auth

import com.example.petcare.data.model.UpdateUserRequest
import com.example.petcare.data.model.User
import com.example.petcare.data.network.ApiService

class UserRepository(private val api: ApiService) {

    suspend fun getMe(): Result<User> = runCatching {
        val response = api.getMe()
        response.body() ?: error("Empty response")
    }


    suspend fun updateMe(request: UpdateUserRequest): Result<User> = runCatching {
        val response = api.updateMe(request)
        response.body() ?: error("Empty response")
    }

    suspend fun deleteMe(): Result<Unit> = runCatching {
        api.deleteMe()
    }
}