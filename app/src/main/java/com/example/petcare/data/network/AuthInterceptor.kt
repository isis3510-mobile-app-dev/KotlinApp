package com.example.petcare.data.network

import com.example.petcare.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val authRepository: AuthRepository,
    private val onSessionExpired: () -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            runCatching { authRepository.getFreshToken() }.getOrNull()
        }

        val requestBuilder = chain.request().newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            response.close()
            val freshToken = runBlocking {
                runCatching { authRepository.forceRefreshToken() }.getOrNull()
            }

            return if (!freshToken.isNullOrBlank()) {
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $freshToken")
                        .build()
                )
            } else {
                onSessionExpired()
                response
            }
        }

        return response
    }
}