package com.example.petcare.data.network

import com.example.petcare.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Cambia esta URL según dónde corra tu backend:
    //   Emulador Android: "http://10.0.2.2:8000/api/"
    //   Dispositivo físico: "http://192.168.x.x:8000/api/"
    private val BASE_URL = com.example.petcare.BuildConfig.BASE_URL

    val sessionExpiredFlow = MutableStateFlow(false)

    fun create(authRepository: AuthRepository): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(
                authRepository = authRepository,
                onSessionExpired = {
                    authRepository.logout()
                    sessionExpiredFlow.value = true
                }
            ))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}