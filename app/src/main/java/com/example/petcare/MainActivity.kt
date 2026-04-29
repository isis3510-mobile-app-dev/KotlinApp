package com.example.petcare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.petcare.data.nfc.NfcManager
import com.example.petcare.data.analytics.AnalyticsSeeder
import com.example.petcare.data.analytics.ScreenTimeTracker
import com.example.petcare.data.network.ApiClient
import com.example.petcare.data.notifications.NotificationDispatcher
import com.example.petcare.data.notifications.NotificationScheduler
import com.example.petcare.ui.preferences.AppThemeViewModel
import com.example.petcare.data.repository.RepositoryProvider
import com.example.petcare.ui.components.ExpandableFAB
import com.example.petcare.ui.components.NavBar
import com.example.petcare.ui.navigation.Routes
import com.example.petcare.ui.navigation.bottomBarRoutes
import com.example.petcare.ui.screens.addEventForm.AddEventDetailsForm
import com.example.petcare.ui.screens.addEventForm.AddEventFinalForm
import com.example.petcare.ui.screens.addEventForm.AddEventInitialForm
import com.example.petcare.ui.screens.addEventForm.AddEventViewModel
import com.example.petcare.ui.screens.addPetForm.AddPetDetailsForm
import com.example.petcare.ui.screens.addPetForm.AddPetFinalForm
import com.example.petcare.ui.screens.addPetForm.AddPetInitialForm
import com.example.petcare.ui.screens.addPetForm.AddPetViewModel
import com.example.petcare.ui.screens.addVaccineForm.AddVaccineDetailsForm
import com.example.petcare.ui.screens.addVaccineForm.AddVaccineFinalForm
import com.example.petcare.ui.screens.addVaccineForm.AddVaccineInitialForm
import com.example.petcare.ui.screens.addVaccineForm.AddVaccineViewModel
import com.example.petcare.ui.screens.auth.AuthViewModel
import com.example.petcare.ui.screens.auth.LoginScreen
import com.example.petcare.ui.screens.auth.SignInScreen
import com.example.petcare.ui.screens.calendar.CalendarScreen
import com.example.petcare.ui.screens.home.HomeScreen
import com.example.petcare.ui.screens.home.HomeViewModel
import com.example.petcare.ui.screens.nfc.NfcViewModel
import com.example.petcare.ui.screens.nfc.ScanNFCScreen
import com.example.petcare.ui.screens.nfc.ScannedSuccessScreen
import com.example.petcare.ui.screens.nfc.ScanningNFCScreen
import com.example.petcare.ui.screens.nfc.TagWrittenScreen
import com.example.petcare.ui.screens.nfc.WriteNFCScreen
import com.example.petcare.ui.screens.onboarding.OnBoardingScreen
import com.example.petcare.ui.screens.petprofile.PetProfileScreen
import com.example.petcare.ui.screens.petprofile.PetProfileViewModel
import com.example.petcare.ui.screens.petprofile.events.EventDetailsScreen
import com.example.petcare.ui.screens.petprofile.vaccines.VaccineDetailsScreen
import com.example.petcare.ui.screens.pets.PetsScreen
import com.example.petcare.ui.screens.pets.PetsViewModel
import com.example.petcare.ui.screens.pets.PetsViewModelFactory
import com.example.petcare.ui.screens.profile.ProfileScreen
import com.example.petcare.ui.screens.profile.ProfileViewModel
import com.example.petcare.ui.screens.records.HealthRecordsScreen
import com.example.petcare.ui.screens.suggestions.SuggestionScreen
import com.example.petcare.ui.screens.weight.WeightTrackerScreen
import com.example.petcare.ui.theme.GreenAccentDark
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.LocalAppThemeMode
import com.example.petcare.ui.theme.OnboardingBlueEnd
import com.example.petcare.ui.theme.OnboardingBlueStart
import com.example.petcare.ui.theme.OnboardingPurpleEnd
import com.example.petcare.ui.theme.OnboardingPurpleStart
import com.example.petcare.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock

class MainActivity : ComponentActivity() {

    private data class PendingNotificationTarget(
        val route: String,
        val backendNotificationId: String?
    )

    private val appThemeViewModel: AppThemeViewModel by viewModels {
        val app = application as PetCareApplication
        ViewModelFactory(app.userPreferencesRepository)
    }

    lateinit var nfcManager: NfcManager
        private set
    val nfcViewModel: NfcViewModel by viewModels {
        NfcViewModel.NfcViewModelFactory(application)
    }

    val petsViewModel: PetsViewModel by viewModels {
        PetsViewModelFactory(RepositoryProvider.petRepository)
    }

    private val pendingNotificationTarget = MutableStateFlow<PendingNotificationTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcManager = NfcManager(this)
        extractNotificationTarget(intent)?.let {
            pendingNotificationTarget.value = it
            intent.action = null
        }

        setContent {
            val themeMode by appThemeViewModel.themeMode.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            val authViewModel: AuthViewModel = viewModel()
            val homeViewModel: HomeViewModel = viewModel()
            val addPetViewModel: AddPetViewModel = viewModel()
            val addVaccineViewModel: AddVaccineViewModel = viewModel()
            val addEventViewModel: AddEventViewModel = viewModel()
            val uiScope = rememberCoroutineScope()
            val notificationsEnabled by (application as PetCareApplication)
                .userPreferencesRepository
                .notificationsEnabled
                .collectAsStateWithLifecycle(initialValue = false)
            val pendingTarget by pendingNotificationTarget.asStateFlow().collectAsStateWithLifecycle()
            val firebaseUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { }
            val sessionExpired by ApiClient.sessionExpiredFlow.collectAsStateWithLifecycle()

            fun navigateToBottomTab(route: String, skipIfCurrent: Boolean = true) {
                if (skipIfCurrent && currentRoute == route) return
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = false
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }

            fun completeAddEventFlow(originRoute: String) {
                runCatching { navController.getBackStackEntry(Routes.Records) }
                    .onSuccess { it.savedStateHandle["reload_records"] = true }
                runCatching { navController.getBackStackEntry(Routes.Home) }
                    .onSuccess { it.savedStateHandle["reload_home"] = true }
                runCatching { navController.getBackStackEntry(Routes.Calendar) }
                    .onSuccess { it.savedStateHandle["reload_calendar"] = true }

                if (originRoute.startsWith("petProfile/")) {
                    runCatching { navController.getBackStackEntry(originRoute) }
                        .onSuccess { it.savedStateHandle["reload_pet"] = true }

                    if (!navController.popBackStack(originRoute, inclusive = false)) {
                        navController.navigate(originRoute) {
                            launchSingleTop = true
                            popUpTo(Routes.AddEvent1) { inclusive = true }
                        }
                    }
                    return
                }

                val targetRoute = originRoute.takeIf { it in bottomBarRoutes } ?: Routes.Records
                navigateToBottomTab(targetRoute, skipIfCurrent = false)
            }


            LaunchedEffect(authViewModel.isLoggedIn) {
                if (authViewModel.isLoggedIn) {
                    authViewModel.fetchUserProfile()
                    authViewModel.syncEmailWithBackend()

                    // ── Analytics: verify+seed metadata for this session ──
                    AnalyticsSeeder.seedIfNeeded(this@MainActivity)
                }
            }

            LaunchedEffect(firebaseUserId) {
                homeViewModel.clearSessionData()
                nfcViewModel.resetSession()

                if (firebaseUserId.isNullOrBlank()) {
                    petsViewModel.clearSessionData()
                } else {
                    petsViewModel.refresh()
                }
            }

            LaunchedEffect(authViewModel.isLoggedIn, notificationsEnabled) {
                if (!authViewModel.isLoggedIn || !notificationsEnabled) return@LaunchedEffect

                // Trigger an immediate check so reminders are testable without waiting
                // for the next periodic WorkManager execution window.
                NotificationScheduler.runNow(this@MainActivity)

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect

                val permissionGranted = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (!permissionGranted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            LaunchedEffect(pendingTarget, authViewModel.isLoggedIn) {
                val target = pendingTarget ?: return@LaunchedEffect
                if (!authViewModel.isLoggedIn) return@LaunchedEffect

                target.backendNotificationId?.let { notificationId ->
                    RepositoryProvider.notificationRepository.markNotificationClicked(
                        notificationId = notificationId,
                        clickedAtIso = Instant.now().toString()
                    )
                }

                navController.navigate(target.route) {
                    launchSingleTop = true
                }
                pendingNotificationTarget.value = null
            }

            // ── Analytics: track screen time ──
            val screenTimeTracker = remember(authViewModel.isLoggedIn, firebaseUserId) {
                if (authViewModel.isLoggedIn && !firebaseUserId.isNullOrBlank()) {
                    ScreenTimeTracker(userId = firebaseUserId)
                } else {
                    null
                }
            }

            DisposableEffect(navController, screenTimeTracker) {
                if (screenTimeTracker != null) {
                    screenTimeTracker.attach(navController)
                }
                onDispose {
                    screenTimeTracker?.detach(navController)
                    screenTimeTracker?.flush()
                }
            }

            CompositionLocalProvider(LocalAppThemeMode provides themeMode) {
                PetCareTheme(themeMode = themeMode) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            if (currentRoute in bottomBarRoutes) {
                                NavBar(
                                    currentRoute = currentRoute ?: Routes.Home,
                                    onItemClick  = { route ->
                                        navigateToBottomTab(route)
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            NavHost(
                                navController    = navController,
                                startDestination = if (authViewModel.isLoggedIn) Routes.Home else Routes.Onboarding1,
                                modifier         = Modifier.padding(innerPadding)
                            ) {
                                // ── Onboarding ────────────────────────────────────────────
                                composable(Routes.Onboarding1) {
                                    OnBoardingScreen(
                                        page = 1,
                                        title = "All your pet's health, \n in one place",
                                        description = "Centralize vaccines, medications, records, and documents.",
                                        backgroundStart = GreenDark, backgroundEnd = GreenAccentDark,
                                        image = R.drawable.onboarding_dop,
                                        onSignInClick  = { navController.navigate(Routes.SignIn) { popUpTo(Routes.Onboarding1) { inclusive = true } } },
                                        onSkipClick    = { navController.navigate(Routes.SignIn) { popUpTo(Routes.Onboarding1) { inclusive = true } } },
                                        onContinueClick = { navController.navigate(Routes.Onboarding2) }
                                    )
                                }
                                composable(Routes.Onboarding2) {
                                    OnBoardingScreen(
                                        page = 2,
                                        title = "Track vaccines & \n medications",
                                        description = "Timeline-based vaccine history, smart reminders, and overdue alerts.",
                                        backgroundStart = OnboardingBlueStart, backgroundEnd = OnboardingBlueEnd,
                                        image = R.drawable.onboarding_vaccine,
                                        onSignInClick  = { navController.navigate(Routes.SignIn) { popUpTo(Routes.Onboarding2) { inclusive = true } } },
                                        onSkipClick    = { navController.navigate(Routes.SignIn) { popUpTo(Routes.Onboarding2) { inclusive = true } } },
                                        onContinueClick = { navController.navigate(Routes.Onboarding3) }
                                    )
                                }
                                composable(Routes.Onboarding3) {
                                    OnBoardingScreen(
                                        page = 3,
                                        title = "NFC tag \n integration",
                                        description = "Write your pet's info to an NFC tag.",
                                        backgroundStart = OnboardingPurpleStart, backgroundEnd = OnboardingPurpleEnd,
                                        image = R.drawable.onboarding_nfc,
                                        onSignInClick  = { navController.navigate(Routes.SignIn) { popUpTo(Routes.Onboarding3) { inclusive = true } } },
                                        onSkipClick    = { navController.navigate(Routes.SignIn) { popUpTo(Routes.Onboarding3) { inclusive = true } } },
                                        onContinueClick = { navController.navigate(Routes.SignIn) }
                                    )
                                }

                                // ── Auth ──────────────────────────────────────────────────
                                composable(Routes.SignIn) {
                                    SignInScreen(
                                        viewModel = authViewModel,
                                        onSignInSuccess = {
                                            uiScope.launch {
                                                AnalyticsSeeder.seedIfNeeded(this@MainActivity)
                                            }
                                            navController.navigate(Routes.Home) {
                                                popUpTo(Routes.SignIn) { inclusive = true }
                                            }
                                        },
                                        onGoToSignUp = { navController.navigate(Routes.SignUp) }
                                    )
                                }
                                composable(Routes.SignUp) {
                                    LoginScreen(
                                        onSignUpSuccess = {
                                            uiScope.launch {
                                                AnalyticsSeeder.seedIfNeeded(this@MainActivity)
                                            }
                                            navController.navigate(Routes.Home) {
                                                popUpTo(Routes.SignUp) { inclusive = true }
                                            }
                                        },
                                        onGoToSignIn = { navController.popBackStack() }
                                    )
                                }

                                // ── Home ──────────────────────────────────────────────────
                                composable(Routes.Home) { entry ->
                                    val reloadHome by entry.savedStateHandle
                                        .getStateFlow("reload_home", false)
                                        .collectAsStateWithLifecycle()

                                    LaunchedEffect(reloadHome) {
                                        if (reloadHome) {
                                            homeViewModel.loadData()
                                            entry.savedStateHandle["reload_home"] = false
                                        }
                                    }

                                    HomeScreen(
                                        authViewModel           = authViewModel,
                                        homeViewModel           = homeViewModel,
                                        onNavigateToPets        = { navigateToBottomTab(Routes.Pets, skipIfCurrent = false) },
                                        onNavigateToNfc         = { navController.navigate(Routes.NfcScan) },
                                        onNavigateToPetProfile  = { petId -> navController.navigate("petProfile/$petId") },
                                        onNavigateToAddPet      = { navController.navigate(Routes.AddPet1) },
                                        onNavigateToVaccine     = { petId, vaccineId ->
                                            navController.navigate("vaccineDetails/$petId/$vaccineId")
                                        },
                                        onNavigateToEvent       = { petId, eventId ->
                                            navController.navigate("eventDetails/$petId/$eventId")
                                        },
                                        onNavigateToRecords     = { navigateToBottomTab(Routes.Records, skipIfCurrent = false) },
                                        onNavigateToSuggestions = { navController.navigate(Routes.Suggestions) }
                                    )
                                }

                                // ── Pets ──────────────────────────────────────────────────
                                composable(Routes.Pets) { entry ->
                                    val reloadPets by entry.savedStateHandle
                                        .getStateFlow("reload_pets", true)
                                        .collectAsStateWithLifecycle()

                                    LaunchedEffect(reloadPets) {
                                        if (reloadPets) {
                                            petsViewModel.refresh()
                                            entry.savedStateHandle["reload_pets"] = false
                                        }
                                    }

                                    val uiState by petsViewModel.uiState.collectAsStateWithLifecycle()

                                    PetsScreen(
                                        pets                = uiState.pets,
                                        isLoading           = uiState.isLoading,
                                        searchQuery         = uiState.searchQuery,
                                        onSearchQueryChange = petsViewModel::updateSearchQuery,
                                        selectedFilter      = uiState.selectedFilter,
                                        onFilterSelected    = petsViewModel::updateSelectedFilter,
                                        onPetSelected       = { petId ->
                                            navController.navigate("petProfile/$petId")
                                        },
                                        onLostModeSelected  = { petId ->
                                            petsViewModel.toggleLostMode(petId)
                                        },
                                        onNfcSelected       = { petId ->
                                            nfcViewModel.prepareWrite(petId, "")
                                            navController.navigate(Routes.NfcScanning)
                                        }
                                    )
                                }

                                // ── Records ───────────────────────────────────────────────
                                composable(Routes.Records) { entry ->
                                    val reloadRecords by entry.savedStateHandle
                                        .getStateFlow("reload_records", false)
                                        .collectAsStateWithLifecycle()

                                    HealthRecordsScreen(
                                        reloadTrigger             = reloadRecords,
                                        onReloadConsumed          = {
                                            entry.savedStateHandle["reload_records"] = false
                                        },
                                        onNavigateToVaccineDetail = { petId, vaccinationId ->
                                            navController.navigate("vaccineDetails/$petId/$vaccinationId")
                                        },
                                        onNavigateToEventDetail   = { petId, eventId ->
                                            navController.navigate("eventDetails/$petId/$eventId")
                                        },
                                        onAddRecordClick  = {
                                            addEventViewModel.reset()
                                            val userId = authViewModel.userProfile.value?.id ?: ""
                                            addEventViewModel.setOwnerId(userId)
                                            addEventViewModel.setOriginRoute(Routes.Records)
                                            navController.navigate(Routes.AddEvent1)
                                        },
                                        onAddVaccineClick = {
                                            addVaccineViewModel.reset()
                                            addVaccineViewModel.setPetId("") // Clear previous pet if any
                                            navController.navigate(Routes.AddVaccine1)
                                        },
                                        onAddEventClick   = {
                                            addEventViewModel.reset()
                                            val userId = authViewModel.userProfile.value?.id ?: ""
                                            addEventViewModel.setOwnerId(userId)
                                            addEventViewModel.setOriginRoute(Routes.Records)
                                            navController.navigate(Routes.AddEvent1)
                                        }
                                    )
                                }

                                // ── Calendar ──────────────────────────────────────────────
                                composable(Routes.Calendar) { entry ->
                                    val reloadCalendar by entry.savedStateHandle
                                        .getStateFlow("reload_calendar", false)
                                        .collectAsStateWithLifecycle()
                                    CalendarScreen(
                                        reloadTrigger = reloadCalendar,
                                        onReloadConsumed = {
                                            entry.savedStateHandle["reload_calendar"] = false
                                        },
                                        onAddEvent = {
                                            addEventViewModel.reset()
                                            val userId = authViewModel.userProfile.value?.id ?: ""
                                            addEventViewModel.setOwnerId(userId)
                                            addEventViewModel.setOriginRoute(Routes.Calendar)
                                            navController.navigate(Routes.AddEvent1)
                                        },
                                        onNavigateToEvent = { petId, eventId ->
                                            navController.navigate("eventDetails/$petId/$eventId")
                                        }
                                    )
                                }

                                // ── Profile ───────────────────────────────────────────────
                                composable(Routes.Profile) { entry ->
                                    LaunchedEffect(Unit) {
                                        authViewModel.syncEmailWithBackend()
                                    }

                                    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
                                    val petsUiState by petsViewModel.uiState.collectAsStateWithLifecycle()

                                    val profileViewModel: ProfileViewModel = viewModel(
                                        factory = ViewModelFactory(
                                            repository  = (applicationContext as PetCareApplication).userPreferencesRepository,
                                            initialUser = userProfile
                                        )
                                    )

                                    // Recarga cuando otra pantalla escribe "reload_profile"
                                    val reloadProfile by entry.savedStateHandle
                                        .getStateFlow("reload_profile", false)
                                        .collectAsStateWithLifecycle()

                                    LaunchedEffect(reloadProfile) {
                                        if (reloadProfile) {
                                            profileViewModel.loadUserProfile()
                                            entry.savedStateHandle["reload_profile"] = false
                                        }
                                    }

                                    // Recarga cada vez que el usuario navega a esta tab
                                    DisposableEffect(entry) {
                                        val observer = LifecycleEventObserver { _, event ->
                                            if (event == Lifecycle.Event.ON_RESUME) {
                                                profileViewModel.loadUserProfile()
                                            }
                                        }
                                        entry.lifecycle.addObserver(observer)
                                        onDispose {
                                            entry.lifecycle.removeObserver(observer)
                                        }
                                    }

                                    ProfileScreen(
                                        viewModel = profileViewModel,
                                        petCount = petsUiState.pets.size,
                                        onSaveSuccess = { authViewModel.fetchUserProfile() },
                                        onNavigateToLogin = {
                                            authViewModel.logout()
                                            if (navController.currentDestination?.route != Routes.SignIn) {
                                                navController.navigate(Routes.SignIn) {
                                                    popUpTo(navController.graph.id) {
                                                        inclusive = true
                                                    }
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    )
                                }

                                // ── Pet Profile ───────────────────────────────────────────
                                composable(
                                    route = Routes.PetProfile,
                                    arguments = listOf(navArgument("petId") { type = NavType.StringType })
                                ) { entry ->
                                    val petId = entry.arguments?.getString("petId").orEmpty()

                                    val reloadPet by entry.savedStateHandle
                                        .getStateFlow("reload_pet", false)
                                        .collectAsStateWithLifecycle()

                                    val petProfileViewModel: PetProfileViewModel = viewModel(entry)

                                    LaunchedEffect(reloadPet) {
                                        if (reloadPet) {
                                            petProfileViewModel.reloadPet()
                                            entry.savedStateHandle["reload_pet"] = false
                                        }
                                    }

                                    PetProfileScreen(
                                        petId        = petId,
                                        viewModel    = petProfileViewModel,
                                        onBack       = {
                                            // Señalamos a Pets, Home y Profile que recarguen
                                            runCatching { navController.getBackStackEntry(Routes.Pets) }
                                                .onSuccess { it.savedStateHandle["reload_pets"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Home) }
                                                .onSuccess { it.savedStateHandle["reload_home"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Profile) }
                                                .onSuccess { it.savedStateHandle["reload_profile"] = true }
                                            navController.popBackStack()
                                        },
                                        onPetDeleted = {
                                            runCatching { navController.getBackStackEntry(Routes.Pets) }
                                                .onSuccess { it.savedStateHandle["reload_pets"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Home) }
                                                .onSuccess { it.savedStateHandle["reload_home"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Profile) }
                                                .onSuccess { it.savedStateHandle["reload_profile"] = true }
                                            homeViewModel.removeDeletedPet(petId)
                                            petsViewModel.removeDeletedPet(petId)
                                            navController.navigate(Routes.Home) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    inclusive = false
                                                }
                                                launchSingleTop = true
                                            }
                                        },
                                        onAddEvent   = {
                                            addEventViewModel.reset()
                                            addEventViewModel.setPetId(petId)
                                            addEventViewModel.setOwnerId(
                                                authViewModel.userProfile.value?.id ?: ""
                                            )
                                            addEventViewModel.setOriginRoute("petProfile/$petId")
                                            navController.navigate(Routes.AddEvent1)
                                        },
                                        onNFCScan    = { navController.navigate(Routes.NfcScan) },
                                        onAddVaccine = {
                                            addVaccineViewModel.reset()
                                            addVaccineViewModel.setPetId(petId)
                                            navController.navigate(Routes.AddVaccine1)
                                        },
                                        onWeightTracker = {
                                            navController.navigate("weightTracker/$petId")
                                        },
                                        onNavigateToVaccineDetail = { pId, vaccineId ->
                                            navController.navigate("vaccineDetails/$pId/$vaccineId")
                                        },
                                        onNavigateToEventDetail = { pId, eventId ->
                                            navController.navigate("eventDetails/$pId/$eventId")
                                        },
                                        onSeeAllNotifications = { pid, petName ->
                                            navController.navigate("suggestions/$pid/$petName")
                                        }

                                    )
                                }

                                // ── Weight Tracker ────────────────────────────────────────
                                composable(
                                    route = Routes.WeightTracker,
                                    arguments = listOf(navArgument("petId") { type = NavType.StringType })
                                ) { entry ->
                                    WeightTrackerScreen(
                                        petId = entry.arguments?.getString("petId").orEmpty(),
                                        onBack = {
                                            runCatching { navController.getBackStackEntry(Routes.Home) }
                                                .onSuccess { it.savedStateHandle["reload_home"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Pets) }
                                                .onSuccess { it.savedStateHandle["reload_pets"] = true }
                                            navController.popBackStack()
                                        }
                                    )
                                }

                                // ── Vaccine Details ───────────────────────────────────────
                                composable(
                                    route = Routes.VaccineDetails,
                                    arguments = listOf(
                                        navArgument("petId")     { type = NavType.StringType },
                                        navArgument("vaccineId") { type = NavType.StringType }
                                    )
                                ) { entry ->
                                    VaccineDetailsScreen(
                                        petId     = entry.arguments?.getString("petId").orEmpty(),
                                        vaccineId = entry.arguments?.getString("vaccineId").orEmpty(),
                                        onNavigateBack = {
                                            val prev = navController.previousBackStackEntry?.savedStateHandle
                                            prev?.set("reload_pet",      true)
                                            prev?.set("reload_records",  true)
                                            prev?.set("reload_home",     true)
                                            prev?.set("reload_calendar", true)
                                            runCatching { navController.getBackStackEntry(Routes.Calendar) }
                                                .onSuccess { it.savedStateHandle["reload_calendar"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Records) }
                                                .onSuccess { it.savedStateHandle["reload_records"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Home) }
                                                .onSuccess { it.savedStateHandle["reload_home"] = true }
                                            navController.popBackStack()
                                        }
                                    )
                                }

                                // ── Event Details ─────────────────────────────────────────
                                composable(
                                    route = Routes.EventDetails,
                                    arguments = listOf(
                                        navArgument("petId")    { type = NavType.StringType },
                                        navArgument("eventId")  { type = NavType.StringType }
                                    )
                                ) { entry ->
                                    EventDetailsScreen(
                                        petId   = entry.arguments?.getString("petId").orEmpty(),
                                        eventId = entry.arguments?.getString("eventId").orEmpty(),
                                        onNavigateBack = {
                                            val prev = navController.previousBackStackEntry?.savedStateHandle
                                            prev?.set("reload_pet",      true)
                                            prev?.set("reload_records",  true)
                                            prev?.set("reload_home",     true)
                                            prev?.set("reload_calendar", true)
                                            runCatching { navController.getBackStackEntry(Routes.Calendar) }
                                                .onSuccess { it.savedStateHandle["reload_calendar"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Records) }
                                                .onSuccess { it.savedStateHandle["reload_records"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Home) }
                                                .onSuccess { it.savedStateHandle["reload_home"] = true }
                                            navController.popBackStack()
                                        }
                                    )
                                }

                                // ── NFC ───────────────────────────────────────────────────
                                composable(Routes.NfcScan) {
                                    ScanNFCScreen(
                                        onBack  = { navController.popBackStack() },
                                        onDone  = { navController.navigate(Routes.NfcScanning) },
                                        onWrite = {
                                            navController.navigate(Routes.NfcWrite) {
                                                popUpTo(Routes.NfcScan) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                composable(Routes.NfcScanning) {
                                    ScanningNFCScreen(
                                        onBack         = { navController.popBackStack() },
                                        onWriteSuccess = {
                                            navController.navigate(Routes.NfcWriteSuccess) {
                                                popUpTo(Routes.NfcScanning) { inclusive = true }
                                            }
                                        },
                                        onReadSuccess  = {
                                            navController.navigate(Routes.NfcScanSuccess) {
                                                popUpTo(Routes.NfcScanning) { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                composable(Routes.NfcScanSuccess) {
                                    ScannedSuccessScreen(
                                        onBack = { navController.popBackStack() },
                                        onDone = { navController.popBackStack(Routes.NfcScan, inclusive = true) }
                                    )
                                }
                                composable(Routes.NfcWrite) {
                                    WriteNFCScreen(
                                        onBack  = { navController.popBackStack() },
                                        onDone  = { navController.navigate(Routes.NfcWriting) },
                                        onRead  = {
                                            navController.navigate(Routes.NfcScan) {
                                                popUpTo(Routes.NfcWrite) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                composable(Routes.NfcWriting) {
                                    ScanningNFCScreen(
                                        onBack         = { navController.popBackStack() },
                                        onWriteSuccess = {
                                            navController.navigate(Routes.NfcWriteSuccess) {
                                                popUpTo(Routes.NfcWriting) { inclusive = true }
                                            }
                                        },
                                        onReadSuccess  = {}
                                    )
                                }
                                composable(Routes.NfcWriteSuccess) {
                                    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
                                    TagWrittenScreen(
                                        onBack    = { navController.popBackStack() },
                                        onDone    = { navController.popBackStack(Routes.NfcWrite, inclusive = true) },
                                        onAnother = { navController.popBackStack() },
                                        ownerName  = userProfile?.name  ?: "",
                                        ownerPhone = userProfile?.phone ?: ""
                                    )
                                }

                                // ── Add Pet ───────────────────────────────────────────────
                                composable(Routes.AddPet1) {
                                    AddPetInitialForm(
                                        viewModel = addPetViewModel,
                                        onBack    = { navController.popBackStack() },
                                        onclick   = { navController.navigate(Routes.AddPet2) }
                                    )
                                }
                                composable(Routes.AddPet2) {
                                    AddPetDetailsForm(
                                        viewModel = addPetViewModel,
                                        onBack    = { navController.popBackStack() },
                                        onclick   = { navController.navigate(Routes.AddPet3) }
                                    )
                                }
                                composable(Routes.AddPet3) {
                                    AddPetFinalForm(
                                        viewModel = addPetViewModel,
                                        onBack    = { navController.popBackStack() },
                                        onclick   = { pet ->
                                            petsViewModel.addOrReplacePet(pet)
                                            homeViewModel.addOrReplacePet(pet)
                                            addPetViewModel.reset()
                                            // Señalamos a Pets, Home y Profile que recarguen
                                            runCatching { navController.getBackStackEntry(Routes.Pets) }
                                                .onSuccess { it.savedStateHandle["reload_pets"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Home) }
                                                .onSuccess { it.savedStateHandle["reload_home"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Profile) }
                                                .onSuccess { it.savedStateHandle["reload_profile"] = true }
                                            navController.popBackStack(Routes.AddPet1, inclusive = true)
                                        }
                                    )
                                }

                                // ── Add Vaccine ───────────────────────────────────────────
                                composable(Routes.AddVaccine1) {
                                    LaunchedEffect(Unit) {
                                        petsViewModel.refresh()
                                    }

                                    AddVaccineInitialForm(
                                        viewModel     = addVaccineViewModel,
                                        petsViewModel = petsViewModel,
                                        onBack        = { navController.popBackStack() },
                                        onclick       = { navController.navigate(Routes.AddVaccine2) }
                                    )
                                }
                                composable(Routes.AddVaccine2) {
                                    AddVaccineDetailsForm(
                                        viewModel = addVaccineViewModel,
                                        onBack    = { navController.popBackStack() },
                                        onclick   = { navController.navigate(Routes.AddVaccine3) }
                                    )
                                }
                                composable(Routes.AddVaccine3) {
                                    AddVaccineFinalForm(
                                        viewModel = addVaccineViewModel,
                                        onBack    = { navController.popBackStack() },
                                        onclick   = {
                                            val completedPetId = addVaccineViewModel.state.value.petId
                                            addVaccineViewModel.reset()
                                            // Señalamos a Records, Home y PetProfile que recarguen
                                            runCatching { navController.getBackStackEntry(Routes.Records) }
                                                .onSuccess { it.savedStateHandle["reload_records"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Home) }
                                                .onSuccess { it.savedStateHandle["reload_home"] = true }
                                            runCatching { navController.getBackStackEntry(Routes.Calendar) }
                                                .onSuccess { it.savedStateHandle["reload_calendar"] = true }
                                            runCatching { navController.getBackStackEntry("petProfile/$completedPetId") }
                                                .onSuccess { it.savedStateHandle["reload_pet"] = true }
                                                .onFailure {
                                                    runCatching { navController.getBackStackEntry(Routes.PetProfile) }
                                                        .onSuccess { it.savedStateHandle["reload_pet"] = true }
                                                }
                                            navController.popBackStack(Routes.AddVaccine1, inclusive = true)
                                        }
                                    )
                                }

                                // ── Add Event ─────────────────────────────────────────────
                                composable(Routes.AddEvent1) {
                                    LaunchedEffect(Unit) {
                                        petsViewModel.refresh()
                                    }

                                    AddEventInitialForm(
                                        viewModel     = addEventViewModel,
                                        petsViewModel = petsViewModel,
                                        onBack        = { navController.popBackStack() },
                                        onclick       = { navController.navigate(Routes.AddEvent2) }
                                    )
                                }
                                composable(Routes.AddEvent2) {
                                    AddEventDetailsForm(
                                        viewModel = addEventViewModel,
                                        onBack    = { navController.popBackStack() },
                                        onclick   = { navController.navigate(Routes.AddEvent3) }
                                    )
                                }
                                composable(Routes.AddEvent3) {
                                    AddEventFinalForm(
                                        viewModel = addEventViewModel,
                                        onBack    = { navController.popBackStack() },
                                        onclick   = {
                                            val originRoute = addEventViewModel.state.value.originRoute
                                            addEventViewModel.reset()
                                            completeAddEventFlow(originRoute)
                                        }
                                    )
                                }

                                // ── Suggestions ───────────────────────────────────────────
                                composable(Routes.Suggestions) {
                                    SuggestionScreen(
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = Routes.SuggestionPerPet,
                                    arguments = listOf(
                                        navArgument("petId")   { type = NavType.StringType },
                                        navArgument("petName") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    SuggestionScreen(
                                        filterPetId   = backStackEntry.arguments?.getString("petId"),
                                        filterPetName = backStackEntry.arguments?.getString("petName"),
                                        onBack        = { navController.popBackStack() }
                                    )
                                }
                            }

                            // ── FAB flotante ──────────────────────────────────────────────
                            if (currentRoute in bottomBarRoutes) {
                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                ) {
                                    ExpandableFAB(
                                        onAddPet     = { navController.navigate(Routes.AddPet1) },
                                        onAddVaccine = {
                                            addVaccineViewModel.reset()
                                            navController.navigate(Routes.AddVaccine1)
                                        },
                                        onAddEvent   = {
                                            addEventViewModel.reset()
                                            val userId = authViewModel.userProfile.value?.id ?: ""
                                            addEventViewModel.setOwnerId(userId)
                                            addEventViewModel.setOriginRoute(
                                                currentRoute?.substringBefore("/") ?: Routes.Home
                                            )
                                            navController.navigate(Routes.AddEvent1)
                                        },
                                        onScanNFC    = { navController.navigate(Routes.NfcScan) }
                                    )
                                }
                            }
                        }
                    }

                    if (sessionExpired) {
                        AlertDialog(
                            onDismissRequest = { },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            title = {
                                Text("Session closed")
                            },
                            text = {
                                Text("Your session has expired. Please sign in again to continue.")
                            },
                            confirmButton = {
                                Button(
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary,
                                        disabledContainerColor = MaterialTheme.colorScheme.secondary,
                                        disabledContentColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    onClick = {
                                    ApiClient.sessionExpiredFlow.value = false
                                    authViewModel.logout()
                                }) {
                                    Text(text = "Close",
                                        color = MaterialTheme.colorScheme.surface)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun extractNotificationTarget(intent: Intent?): PendingNotificationTarget? {
        if (intent?.action != NotificationDispatcher.ACTION_OPEN_NOTIFICATION) return null
        val route = intent.getStringExtra(NotificationDispatcher.EXTRA_TARGET_ROUTE) ?: return null
        val backendNotificationId = intent.getStringExtra(NotificationDispatcher.EXTRA_NOTIFICATION_ID)
        return PendingNotificationTarget(
            route = route,
            backendNotificationId = backendNotificationId
        )
    }



    // ── NFC lifecycle ─────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        nfcManager.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationTarget(intent)?.let {
            pendingNotificationTarget.value = it
            intent.action = null
        }
        val action = intent.action ?: return
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED) return

        val tag = nfcManager.getTagFromIntent(intent) ?: return
        if (nfcViewModel.isPendingWrite()) {
            nfcViewModel.onTagDetectedForWrite(tag, nfcManager)
        } else if (nfcViewModel.isReadyForReadTag()) {
            nfcViewModel.onTagDetectedForRead(tag, nfcManager)
        }
    }
}
