package com.example.petcare.data.repository
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

    fun ensureInitialized() {
        if (!::apiService.isInitialized) {
            init(AuthRepository())
        }
    }

    fun init(authRepository: AuthRepository) {
        _authRepository = authRepository
        apiService      = ApiClient.create(authRepository).create(ApiService::class.java)
        petRepository   = PetRepository(apiService)
        eventRepository = EventRepository(apiService)
        nfcRepository   = NfcRepository(apiService)
        notificationRepository = NotificationRepository(apiService)
    }


}
