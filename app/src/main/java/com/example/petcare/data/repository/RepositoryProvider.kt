package com.example.petcare.data.repository

import android.content.Context
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.hive.HiveCacheManager
import com.example.petcare.data.network.ApiClient
import com.example.petcare.data.network.ApiService

object RepositoryProvider {

    lateinit var apiService: ApiService
        private set
    private lateinit var _authRepository: AuthRepository

    val authRepository: AuthRepository get() = _authRepository

    lateinit var petRepository: PetRepository
        private set

    lateinit var eventRepository: EventRepository
        private set

    lateinit var nfcRepository: NfcRepository
        private set

    lateinit var notificationRepository: NotificationRepository
        private set

    fun ensureInitialized(context: Context) {
        if (!::apiService.isInitialized) {
            init(AuthRepository(), context)
        }
    }

    fun init(authRepository: AuthRepository, context: Context) {
        val appContext = context.applicationContext
        val db = AppDatabase.getInstance(appContext)
        val hive = HiveCacheManager(appContext)

        android.util.Log.d("REPO_PROVIDER", "Initializing with db=${db}, petDao=${db.petDao()}")

        _authRepository = authRepository
        apiService      = ApiClient.create(authRepository).create(ApiService::class.java)
        petRepository  = PetRepository(
            petDao     = db.petDao(),
            vaccineDao = db.vaccineDao(),
            api        = apiService,
            context    = appContext,
            hive       = hive
        )
        eventRepository = EventRepository(apiService)
        nfcRepository   = NfcRepository(apiService)
        notificationRepository = NotificationRepository(apiService)
    }


}
