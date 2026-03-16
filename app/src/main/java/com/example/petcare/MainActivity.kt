package com.example.petcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.data.preferences.AppThemeViewModel
import com.example.petcare.ui.screens.home.HomeScreen
import com.example.petcare.ui.screens.profile.ProfileScreen
import com.example.petcare.ui.screens.profile.ProfileViewModel
import com.example.petcare.ui.theme.LocalAppThemeMode
import com.example.petcare.ui.theme.PetCareTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.petcare.ui.components.ExpandableFAB
import com.example.petcare.ui.components.NavBar
import com.example.petcare.ui.navigation.Routes
import com.example.petcare.ui.navigation.bottomBarRoutes
import com.example.petcare.ui.screens.addEventForm.AddEventDetailsForm
import com.example.petcare.ui.screens.addEventForm.AddEventFinalForm
import com.example.petcare.ui.screens.addEventForm.AddEventInitialForm
import com.example.petcare.ui.screens.addPetForm.AddPetDetailsForm
import com.example.petcare.ui.screens.addPetForm.AddPetFinalForm
import com.example.petcare.ui.screens.addPetForm.AddPetInitialForm
import com.example.petcare.ui.screens.addVaccineForm.AddVaccineDetailsForm
import com.example.petcare.ui.screens.addVaccineForm.AddVaccineFinalForm
import com.example.petcare.ui.screens.addVaccineForm.AddVaccineInitialForm
import com.example.petcare.ui.screens.auth.AuthViewModel
import com.example.petcare.ui.screens.auth.LoginScreen
import com.example.petcare.ui.screens.calendar.CalendarScreen
import com.example.petcare.ui.screens.pets.PetsScreen
import com.example.petcare.ui.screens.records.HealthRecordsScreen
import com.example.petcare.ui.screens.auth.SignInScreen
import com.example.petcare.ui.screens.nfc.ScanNFCScreen
import com.example.petcare.ui.screens.nfc.ScannedSuccessScreen
import com.example.petcare.ui.screens.nfc.ScanningNFCScreen
import com.example.petcare.ui.screens.nfc.TagWrittenScreen
import com.example.petcare.ui.screens.nfc.WriteNFCScreen
import com.example.petcare.ui.screens.onboarding.OnBoardingScreen
import com.example.petcare.ui.screens.petprofile.PetProfileScreen
import com.example.petcare.ui.screens.petprofile.events.EventDetailsScreen
import com.example.petcare.ui.screens.petprofile.vaccines.VaccineDetailsScreen
import com.example.petcare.ui.theme.GreenAccentDark
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.OffWhite
import com.example.petcare.ui.theme.OnboardingBlueEnd
import com.example.petcare.ui.theme.OnboardingBlueStart
import com.example.petcare.ui.theme.OnboardingPurpleEnd
import com.example.petcare.ui.theme.OnboardingPurpleStart
import android.content.Intent
import android.nfc.NfcAdapter
import com.example.petcare.data.nfc.NfcManager
import com.example.petcare.ui.screens.nfc.NfcUiState
import com.example.petcare.ui.screens.nfc.NfcViewModel

class MainActivity : ComponentActivity() {

    private val appThemeViewModel: AppThemeViewModel by viewModels {
        val app = application as PetCareApplication
        ViewModelFactory(app.userPreferencesRepository)
    }

    private lateinit var nfcManager: NfcManager
    val nfcViewModel: NfcViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcManager = NfcManager(this)
        setContent {
            val themeMode by appThemeViewModel.themeMode.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val authViewModel : AuthViewModel = viewModel()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            CompositionLocalProvider(LocalAppThemeMode provides themeMode) {
                PetCareTheme(themeMode = themeMode) {
                    Scaffold(
                        containerColor = OffWhite,
                        floatingActionButton = {
                            if (currentRoute in bottomBarRoutes) {
                                ExpandableFAB(
                                    onAddPet =  {navController.navigate(Routes.AddPet1) },
                                    onAddVaccine = {navController.navigate(Routes.AddVaccine1) },
                                    onAddEvent = {navController.navigate(Routes.AddEvent1) },
                                    onScanNFC = {navController.navigate(Routes.NfcScan) }

                                )
                            }
                        },
                        bottomBar = {
                            if (currentRoute in bottomBarRoutes) {
                                NavBar (
                                    currentRoute = currentRoute ?: Routes.Home,
                                    onItemClick = { route ->
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = if (authViewModel.isLoggedIn) Routes.Home else Routes.Onboarding1,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            //ONBOARDING 1
                            composable(Routes.Onboarding1){
                                PetCareTheme {
                                    OnBoardingScreen(
                                        page = 1,
                                        title = "All your pet's health, \n in one place",
                                        description = "Centralize vaccines, medications, records, and documents. Never miss a dose or appointment again.",
                                        backgroundStart = GreenDark,
                                        backgroundEnd = GreenAccentDark,
                                        image = R.drawable.onboarding_dop,
                                        onSignInClick = {navController.navigate(Routes.SignIn){
                                            popUpTo(Routes.Onboarding1) { inclusive = true } } },
                                        onSkipClick = {navController.navigate(Routes.SignIn){ popUpTo(Routes.Onboarding1) { inclusive = true }} },
                                        onContinueClick = {navController.navigate(Routes.Onboarding2) }
                                    )

                                }
                            }

                            //ONBOARDING 2
                            composable(Routes.Onboarding2){
                                PetCareTheme {
                                    OnBoardingScreen(
                                        page = 2,
                                        title = "Track vaccines & \n medications",
                                        description = "Timeline-based vaccine history, smart reminders for medications, and overdue alerts that keep you informed.",
                                        backgroundStart = OnboardingBlueStart,
                                        backgroundEnd = OnboardingBlueEnd,
                                        image = R.drawable.onboarding_vaccine,
                                        onSignInClick = {navController.navigate(Routes.SignIn){ popUpTo(Routes.Onboarding2) { inclusive = true }} },
                                        onSkipClick = {navController.navigate(Routes.SignIn){ popUpTo(Routes.Onboarding2) { inclusive = true }} },
                                        onContinueClick = {navController.navigate(Routes.Onboarding3) }
                                    )

                                }
                            }

                            //ONBOARDING 3
                            composable(Routes.Onboarding3){
                                PetCareTheme {
                                    OnBoardingScreen(
                                        page = 3,
                                        title = "NFC tag \n integration",
                                        description = "Write your pet's info to an NFC tag. Anyone who finds your pet can contact you instantly.",
                                        backgroundStart = OnboardingPurpleStart,
                                        backgroundEnd = OnboardingPurpleEnd,
                                        image = R.drawable.onboarding_nfc,
                                        onSignInClick = {navController.navigate(Routes.SignIn){ popUpTo(Routes.Onboarding3) { inclusive = true }} },
                                        onSkipClick = {navController.navigate(Routes.SignIn){ popUpTo(Routes.Onboarding3) { inclusive = true }} },
                                        onContinueClick = {navController.navigate(Routes.SignIn) }
                                    )

                                }
                            }


                            // SIGN IN ROUTE
                            composable(Routes.SignIn) {
                                SignInScreen(
                                    viewModel = authViewModel,
                                    onSignInSuccess = {
                                        navController.navigate(Routes.Home) {
                                            popUpTo(Routes.SignIn) { inclusive = true }
                                        }
                                    },
                                    onGoToSignUp = { navController.navigate(Routes.SignUp) }
                                )
                            }

                            // SIGN UP ROUTE
                            composable(Routes.SignUp) {
                                LoginScreen(
                                    //viewModel = authViewModel,
                                    onSignUpSuccess = {
                                        navController.navigate(Routes.Home) {
                                            popUpTo(Routes.SignUp) { inclusive = true }
                                        }
                                    },
                                    onGoToSignIn = { navController.popBackStack() }
                                )
                            }

                            // HOME ROUTE
                            composable(Routes.Home) {
                                HomeScreen(
                                    onNavigateToPets = { navController.navigate(Routes.Pets) },
                                    onNavigateToNfc = { navController.navigate(Routes.NfcScan) },
                                    onNavigateToPetProfile = { petId ->
                                        navController.navigate("petProfile/$petId")
                                    },
                                    onNavigateToAddPet = { navController.navigate(Routes.AddPet1) },
                                    onNavigateToVaccine = { petId, vaccineId ->
                                        navController.navigate("vaccineDetails/$petId/$vaccineId")
                                    },
                                    onNavigateToEvent = { petId, eventId ->
                                        navController.navigate("eventDetails/$petId/$eventId")
                                    },
                                    onNavigateToRecords = { navController.navigate(Routes.Records) }
                                )
                            }

                            // PETS ROUTE
                            composable(Routes.Pets) {
                                PetsScreen(
                                    paddingValues = innerPadding,
                                    onPetSelected = { petId ->
                                        navController.navigate("petProfile/$petId")
                                    },
                                    onVaccineSelected = { petId ->
                                        navController.navigate("petProfile/$petId")
                                    },
                                    onLostModeSelected = { },
                                    onNfcSelected = {
                                        navController.navigate(Routes.NfcWrite)
                                    }
                                )
                            }

                            // RECORDS ROUTE
                            composable(Routes.Records) {
                                HealthRecordsScreen()
                            }

                            // CALENDAR ROUTE
                            composable(Routes.Calendar) {
                                CalendarScreen(onAddEvent = {navController.navigate(Routes.AddEvent1)}
                                )
                            }

                            // PROFILE ROUTE
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
                                        navController.navigate(Routes.SignIn) {
                                            popUpTo(0)
                                        }
                                    }
                                )
                            }

                            // NFC SCAN ROUTE
                            composable(Routes.NfcScan) {
                                ScanNFCScreen(
                                    onBack = { navController.popBackStack() },
                                    onDone = { navController.navigate(Routes.NfcScanning) },
                                    onWrite = { navController.navigate(Routes.NfcWrite)}
                                )
                            }

                            composable(Routes.NfcScanning) {
                                ScanningNFCScreen(
                                    onBack = { navController.popBackStack() },
                                    onWriteSuccess = {
                                        navController.navigate(Routes.NfcWriteSuccess) {
                                            popUpTo(Routes.NfcScanning) { inclusive = true }
                                        }
                                    },
                                    onReadSuccess = {
                                        navController.navigate(Routes.NfcScanSuccess) {
                                            popUpTo(Routes.NfcScanning) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Routes.NfcScanSuccess) {
                                ScannedSuccessScreen(
                                    onBack = {
                                        navController.navigate(Routes.NfcScan) {
                                            popUpTo(Routes.NfcScan) { inclusive = false }
                                        }
                                    },
                                    onDone = {
                                        navController.navigate(Routes.NfcScan) {
                                            popUpTo(Routes.NfcScan) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // NFC WRITE ROUTE
                            composable(Routes.NfcWrite) {
                                WriteNFCScreen(
                                    onBack = { navController.popBackStack() },
                                    onDone = { navController.navigate(Routes.NfcWriting) },
                                    onRead = { navController.navigate(Routes.NfcScan)}
                                )
                            }

                            composable(Routes.NfcWriting) {
                                ScanningNFCScreen(
                                    onBack = { navController.popBackStack() },
                                    onWriteSuccess = {
                                        navController.navigate(Routes.NfcWriteSuccess) {
                                            popUpTo(Routes.NfcWriting) { inclusive = true }
                                        }
                                    },
                                    onReadSuccess = { }
                                )
                            }

                            composable(Routes.NfcWriteSuccess) {
                                TagWrittenScreen(
                                    onBack = {
                                        navController.navigate(Routes.NfcWrite) {
                                            popUpTo(Routes.NfcWrite) { inclusive = false }
                                        }
                                    },
                                    onDone = {
                                        navController.navigate(Routes.Home) {
                                            popUpTo(Routes.NfcWrite) { inclusive = true }
                                        }
                                    },
                                    onAnother = {
                                        navController.navigate(Routes.NfcWrite) {
                                            popUpTo(Routes.NfcWrite) { inclusive = true }
                                        }
                                    }

                                )
                            }

                            // PET PROFILE ROUTE
                            composable(
                                route = Routes.PetProfile,
                                arguments = listOf(navArgument("petId") { type = NavType.StringType })
                            ) { entry ->
                                val petId = entry.arguments?.getString("petId").orEmpty()
                                PetProfileScreen(
                                    petId = petId,
                                    onBack = { navController.popBackStack()},
                                    onAddEvent = { navController.navigate(Routes.AddEvent1)},
                                    onNFCScan = { navController.navigate(Routes.NfcScan)},
                                    onAddVaccine = { navController.navigate(Routes.AddVaccine1)},
                                )
                            }

                            // PET VACCINE PROFILE ROUTE
                            composable(
                                route = Routes.VaccineDetails,
                                arguments = listOf(
                                    navArgument("petId") { type = NavType.StringType },
                                    navArgument("vaccineId") { type = NavType.StringType }
                                )
                            ) { entry ->
                                val petId = entry.arguments?.getString("petId").orEmpty()
                                val vaccineId = entry.arguments?.getString("vaccineId").orEmpty()
                                VaccineDetailsScreen(
                                    petId = petId,
                                    vaccineId = vaccineId,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            // PET EVENT PROFILE ROUTE
                            composable(
                                route = Routes.EventDetails,
                                arguments = listOf(
                                    navArgument("petId") { type = NavType.StringType },
                                    navArgument("eventId") { type = NavType.StringType }
                                )
                            ) { entry ->
                                val petId = entry.arguments?.getString("petId").orEmpty()
                                val eventId = entry.arguments?.getString("eventId").orEmpty()
                                EventDetailsScreen(
                                    petId = petId,
                                    eventId = eventId,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            // ADD PETS ROUTE
                            composable(Routes.AddPet1) {
                                AddPetInitialForm(
                                    onBack = { navController.popBackStack() },
                                    onclick = { navController.navigate(Routes.AddPet2)}
                                )
                            }

                            composable(Routes.AddPet2) {
                                AddPetDetailsForm(
                                    onBack = { navController.popBackStack() },
                                    onclick = { navController.navigate(Routes.AddPet3)}
                                )
                            }

                            composable(Routes.AddPet3) {
                                AddPetFinalForm(
                                    onBack = { navController.popBackStack() },
                                    onclick = { navController.navigate(Routes.Pets){
                                        popUpTo(Routes.AddPet1) { inclusive = true }
                                    } }
                                )
                            }

                            // ADD EVENTS ROUTE
                            composable(Routes.AddEvent1) {
                                AddEventInitialForm(
                                    onBack = { navController.popBackStack() },
                                    onclick = { navController.navigate(Routes.AddEvent2)}
                                )
                            }

                            composable(Routes.AddEvent2) {
                                AddEventDetailsForm(
                                    onBack = { navController.popBackStack() },
                                    onclick = { navController.navigate(Routes.AddEvent3)}
                                )
                            }

                            composable(Routes.AddEvent3) {
                                AddEventFinalForm(
                                    onBack = { navController.popBackStack() },
                                    onclick = { navController.navigate(Routes.PetProfile){
                                        popUpTo(Routes.AddEvent1) { inclusive = true }
                                    } }
                                )
                            }

                            // ADD VACCINE ROUTE
                            composable(Routes.AddVaccine1) {
                                AddVaccineInitialForm(
                                    onBack = { navController.popBackStack() },
                                    onclick = { navController.navigate(Routes.AddVaccine2)}
                                )
                            }

                            composable(Routes.AddVaccine2) {
                                AddVaccineDetailsForm(
                                    onBack = { navController.popBackStack() },
                                    onclick = { navController.navigate(Routes.AddVaccine3)}
                                )
                            }

                            composable(Routes.AddVaccine3) {
                                AddVaccineFinalForm(
                                    onBack = { navController.popBackStack() },
                                    onclick = { navController.navigate(Routes.PetProfile){
                                        popUpTo(Routes.AddVaccine1) { inclusive = true }
                                    } }
                                )
                            }

                        }
                    }
                }
            }
        }
    }

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
        setIntent(intent)   // keep getIntent() fresh for any other code that reads it

        val action = intent.action ?: return
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED
        ) return

        val tag = nfcManager.getTagFromIntent(intent) ?: return

        if (nfcViewModel.isPendingWrite()) {
            nfcViewModel.onTagDetectedForWrite(tag, nfcManager)
        } else {
            nfcViewModel.onTagDetectedForRead(tag, nfcManager)
        }
    }

}