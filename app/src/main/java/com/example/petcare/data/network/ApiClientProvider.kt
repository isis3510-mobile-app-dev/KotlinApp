package com.example.petcare.data.network

import com.example.petcare.data.repository.AuthRepository

object ApiClientProvider {
    @Volatile
    private var instance: ApiService? = null

    fun getInstance(authRepository: AuthRepository): ApiService {
        return instance ?: synchronized(this) {
            instance ?: ApiClient.create(authRepository)
                .create(ApiService::class.java)
                .also { instance = it }
        }
    }
}