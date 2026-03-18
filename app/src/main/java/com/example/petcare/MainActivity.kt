package com.example.petcare

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.petcare.data.nfc.NfcManager
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
import com.example.petcare.ui.theme.GreenAccentDark
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.LocalAppThemeMode
import com.example.petcare.ui.theme.OffWhite
import com.example.petcare.ui.theme.OnboardingBlueEnd
import com.example.petcare.ui.theme.OnboardingBlueStart
import com.example.petcare.ui.theme.OnboardingPurpleEnd
import com.example.petcare.ui.theme.OnboardingPurpleStart
import com.example.petcare.ui.theme.PetCareTheme

class MainActivity : ComponentActivity() {

    private val appThemeViewModel: AppThemeViewModel by viewModels {
        val app = application as PetCareApplication
        ViewModelFactory(app.userPreferencesRepository)
    }

    private lateinit var nfcManager: NfcManager
    val nfcViewModel: NfcViewModel by viewModels()

    // petsViewModel at class level so WriteNFCScreen can access it via LocalActivity
    val petsViewModel: PetsViewModel by viewModels {
        PetsViewModelFactory(RepositoryProvider.petRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcManager = NfcManager(this)

        setContent {
            val themeMode by appThemeViewModel.themeMode.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            // ── ViewModels whose lifetime is tied to this Activity ─────────────
            val authViewModel: AuthViewModel = viewModel()
            val homeViewModel: HomeViewModel = viewModel()
            val addPetViewModel: AddPetViewModel = viewModel()
            val addVaccineViewModel: AddVaccineViewModel = viewModel()
            val addEventViewModel: AddEventViewModel = viewModel()

            // Fetch the logged-in user's profile once at startup
            LaunchedEffect(Unit) {
                authViewModel.logout()
                if (authViewModel.isLoggedIn) authViewModel.fetchUserProfile()
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
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                            restoreState    = true
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                        }
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

                            // ── Onboarding ────────────────────────────────────────────────────────
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

                            // ── Auth ──────────────────────────────────────────────────────────────
                            composable(Routes.SignIn) {
                                SignInScreen(
                                    viewModel = authViewModel,
                                    onSignInSuccess = {
                                        authViewModel.fetchUserProfile()
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
                                        authViewModel.fetchUserProfile()
                                        navController.navigate(Routes.Home) {
                                            popUpTo(Routes.SignUp) { inclusive = true }
                                        }
                                    },
                                    onGoToSignIn = { navController.popBackStack() }
                                )
                            }

                            // ── Home ──────────────────────────────────────────────────────────────
                            composable(Routes.Home) {
                                HomeScreen(
                                    authViewModel           = authViewModel,
                                    homeViewModel           = homeViewModel,
                                    onNavigateToPets        = { navController.navigate(Routes.Pets) },
                                    onNavigateToNfc         = { navController.navigate(Routes.NfcScan) },
                                    onNavigateToPetProfile  = { petId -> navController.navigate("petProfile/$petId") },
                                    onNavigateToAddPet      = { navController.navigate(Routes.AddPet1) },
                                    onNavigateToVaccine     = { petId, vaccineId -> navController.navigate("vaccineDetails/$petId/$vaccineId") },
                                    onNavigateToEvent       = { petId, eventId   -> navController.navigate("eventDetails/$petId/$eventId") },
                                    onNavigateToRecords     = { navController.navigate(Routes.Records) }
                                )
                            }

                            // ── Pets ──────────────────────────────────────────────────────────────
                            composable(Routes.Pets) {
                                val uiState by petsViewModel.uiState.collectAsStateWithLifecycle()

                                PetsScreen(
                                    pets      = uiState.pets,
                                    isLoading = uiState.isLoading,
                                    searchQuery = uiState.searchQuery,
                                    onSearchQueryChange = petsViewModel::updateSearchQuery,
                                    selectedFilter = uiState.selectedFilter,
                                    onFilterSelected = petsViewModel::updateSelectedFilter,
                                    onPetSelected = { petId ->
                                        navController.navigate("petProfile/$petId")
                                    },
                                    onNfcSelected = { petId ->
                                        nfcViewModel.prepareWrite(petId, "")
                                        navController.navigate(Routes.NfcScanning)
                                    }
                                )
                            }

                            // ── Records / Calendar / Profile ─────────────────────────────────────
                            composable(Routes.Records)  { HealthRecordsScreen() }
                            composable(Routes.Calendar) {
                                CalendarScreen(onAddEvent = { navController.navigate(Routes.AddEvent1) })
                            }
                            composable(Routes.Profile) {
                                val profileViewModel: ProfileViewModel = viewModel(
                                    factory = ViewModelFactory(
                                        (applicationContext as PetCareApplication).userPreferencesRepository
                                    )
                                )
                                ProfileScreen(
                                    viewModel = profileViewModel,
                                    onNavigateToLogin = {
                                        authViewModel.logout()
                                        navController.navigate(Routes.SignIn) { popUpTo(0) }
                                    }
                                )
                            }

                            // ── Pet Profile ───────────────────────────────────────────────────────
                            composable(
                                route = Routes.PetProfile,
                                arguments = listOf(navArgument("petId") { type = NavType.StringType })
                            ) { entry ->
                                val petId = entry.arguments?.getString("petId").orEmpty()
                                val petProfileViewModel: PetProfileViewModel = viewModel()
                                LaunchedEffect(petId) { petProfileViewModel.loadPet(petId) }

                                PetProfileScreen(
                                    petId       = petId,
                                    onBack      = { navController.popBackStack() },
                                    onAddEvent  = {
                                        addEventViewModel.setPetId(petId)
                                        addEventViewModel.setOwnerId(
                                            authViewModel.userProfile.value?.id ?: ""
                                        )
                                        navController.navigate(Routes.AddEvent1)
                                    },
                                    onNFCScan   = { navController.navigate(Routes.NfcScan) },
                                    onAddVaccine = {
                                        addVaccineViewModel.setPetId(petId)
                                        navController.navigate(Routes.AddVaccine1)
                                    }
                                )
                            }

                            // ── Vaccine / Event detail screens ────────────────────────────────────
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
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
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
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            // ── NFC ───────────────────────────────────────────────────────────────
                            composable(Routes.NfcScan) {
                                ScanNFCScreen(
                                    onBack  = { navController.popBackStack() },
                                    onDone  = { navController.navigate(Routes.NfcScanning) },
                                    onWrite = { navController.navigate(Routes.NfcWrite) }
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
                                    onBack = { navController.navigate(Routes.NfcScan) { popUpTo(Routes.NfcScan) { inclusive = false } } },
                                    onDone = { navController.navigate(Routes.NfcScan) { popUpTo(Routes.NfcScan) { inclusive = true  } } }
                                )
                            }
                            composable(Routes.NfcWrite) {
                                WriteNFCScreen(
                                    onBack  = { navController.popBackStack() },
                                    onDone  = { navController.navigate(Routes.NfcWriting) },
                                    onRead  = { navController.navigate(Routes.NfcScan) }
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
                                    onBack    = { navController.navigate(Routes.NfcWrite) { popUpTo(Routes.NfcWrite) { inclusive = false } } },
                                    onDone    = { navController.navigate(Routes.Home)     { popUpTo(Routes.NfcWrite) { inclusive = true  } } },
                                    onAnother = { navController.navigate(Routes.NfcWrite) { popUpTo(Routes.NfcWrite) { inclusive = true  } } },
                                    ownerName  = userProfile?.name  ?: "",
                                    ownerPhone = userProfile?.phone ?: ""
                                )
                            }

                            // ── Add Pet ───────────────────────────────────────────────────────────
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
                                    onclick   = { newPetId ->
                                        addPetViewModel.reset()
                                        navController.navigate("petProfile/$newPetId") {
                                            popUpTo(Routes.AddPet1) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // ── Add Vaccine ───────────────────────────────────────────────────────
                            composable(Routes.AddVaccine1) {
                                AddVaccineInitialForm(
                                    viewModel = addVaccineViewModel,
                                    onBack    = { navController.popBackStack() },
                                    onclick   = { navController.navigate(Routes.AddVaccine2) }
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
                                        val petId = addVaccineViewModel.state.value.petId
                                        addVaccineViewModel.reset()
                                        navController.navigate("petProfile/$petId") {
                                            popUpTo(Routes.AddVaccine1) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // ── Add Event ─────────────────────────────────────────────────────────
                            composable(Routes.AddEvent1) {
                                AddEventInitialForm(
                                    viewModel = addEventViewModel,
                                    onBack    = { navController.popBackStack() },
                                    onclick   = { navController.navigate(Routes.AddEvent2) }
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
                                        val petId = addEventViewModel.state.value.petId
                                        addEventViewModel.reset()
                                        navController.navigate("petProfile/$petId") {
                                            popUpTo(Routes.AddEvent1) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                        
                        if (currentRoute in bottomBarRoutes) {
                            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                ExpandableFAB(
                                    onAddPet     = { navController.navigate(Routes.AddPet1) },
                                    onAddVaccine = { navController.navigate(Routes.AddVaccine1) },
                                    onAddEvent   = { navController.navigate(Routes.AddEvent1) },
                                    onScanNFC    = { navController.navigate(Routes.NfcScan) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
        val action = intent.action ?: return
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED) return

        val tag = nfcManager.getTagFromIntent(intent) ?: return
        if (nfcViewModel.isPendingWrite()) {
            nfcViewModel.onTagDetectedForWrite(tag, nfcManager)
        } else {
            nfcViewModel.onTagDetectedForRead(tag, nfcManager)
        }
    }
}