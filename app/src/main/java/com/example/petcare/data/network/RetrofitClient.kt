package com.example.petcare.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Mismo valor que ApiClient.BASE_URL — cámbialo junto con ese cuando
    // pases a dispositivo físico.
    private const val BASE_URL = "http://10.0.2.2:8000/api/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}