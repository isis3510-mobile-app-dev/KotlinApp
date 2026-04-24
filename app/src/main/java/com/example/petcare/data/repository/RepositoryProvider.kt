package com.example.petcare.data.repository

import android.content.Context
import com.example.petcare.data.local.db.AppDatabase
import com.example.petcare.data.local.hive.HiveCacheManager
import com.example.petcare.data.local.lru.EventLruCache
import com.example.petcare.data.network.ApiClient
import com.example.petcare.data.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

    lateinit var weightLogRepository: WeightLogRepository
        private set

    // Expuesto para poder imprimir stats del LRU (útil para rúbrica)
    lateinit var eventLruCache: EventLruCache
        private set

    fun ensureInitialized(context: Context) {
        if (!::apiService.isInitialized) {
            init(AuthRepository(), context, null)
        }
    }

    fun init(
        authRepository: AuthRepository,
        context: Context,
        externalScope: CoroutineScope?
    ) {
        val appContext = context.applicationContext
        val db         = AppDatabase.getInstance(appContext)
        val hive       = HiveCacheManager(appContext)
        val lru        = EventLruCache(appContext)
        val scope      = externalScope
            ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)

        android.util.Log.d("REPO_PROVIDER", "Initializing with db=${db}, petDao=${db.petDao()}")

        _authRepository = authRepository
        apiService      = ApiClient.create(authRepository).create(ApiService::class.java)

        petRepository = PetRepository(
            petDao        = db.petDao(),
            vaccineDao    = db.vaccineDao(),
            api           = apiService,
            context       = appContext,
            hive          = hive
        )
        eventLruCache   = lru
        eventRepository = EventRepository(
            api           = apiService,
            context       = appContext,
            hive          = hive,
            externalScope = scope,
            lru           = lru
        )
        weightLogRepository = WeightLogRepository(
            weightLogDao = db.weightLogDao(),
            api          = apiService,
            context      = appContext
        )
        nfcRepository          = NfcRepository(apiService)
        notificationRepository = NotificationRepository(apiService)
    }
}