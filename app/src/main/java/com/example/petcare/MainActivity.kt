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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.petcare.ui.components.NavBar
import com.example.petcare.ui.navigation.Routes
import com.example.petcare.ui.navigation.bottomBarRoutes
import com.example.petcare.ui.screens.auth.LoginScreen
import com.example.petcare.ui.screens.calendar.CalendarScreen
import com.example.petcare.ui.screens.pets.PetsScreen
import com.example.petcare.ui.screens.records.HealthRecordsScreen
import com.example.petcare.ui.screens.auth.SignInScreen
import com.example.petcare.ui.screens.nfc.ScanNFCScreen
import com.example.petcare.ui.screens.nfc.WriteNFCScreen
import com.example.petcare.ui.screens.petprofile.PetProfileScreen

class MainActivity : ComponentActivity() {

    private val appThemeViewModel: AppThemeViewModel by viewModels {
        val app = application as PetCareApplication
        ViewModelFactory(app.userPreferencesRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appThemeViewModel.themeMode.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            CompositionLocalProvider(LocalAppThemeMode provides themeMode) {
                PetCareTheme(themeMode = themeMode) {
                    Scaffold(
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
                            startDestination = Routes.SignIn,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            // HOME ROUTE
                            composable(Routes.Home) {
                                HomeScreen(
                                    currentRoute = Routes.Home,
                                    onNavigateTab = { route ->
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                        }
                                    },
                                    onNavigateToPets = { navController.navigate(Routes.Pets) },
                                    onNavigateToNfc = { navController.navigate(Routes.NfcScan) },
                                    onNavigateToPetProfile = { petId ->
                                        navController.navigate("petProfile/$petId")
                                    }
                                )
                            }

                            // PETS ROUTE
                            composable(Routes.Pets) {
                                PetsScreen(
                                    currentRoute = Routes.Pets,
                                    onNavigateTab = { route ->
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                        }
                                    },
                                    onPetSelected = { petId ->
                                        navController.navigate("petProfile/$petId")
                                    }
                                )
                            }

                            // RECORDS ROUTE
                            composable(Routes.Records) {
                                HealthRecordsScreen(
                                    currentRoute = Routes.Records,
                                    onNavigateTab = { route ->
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

                            // CALENDAR ROUTE
                            composable(Routes.Calendar) {
                                CalendarScreen(
                                    currentRoute = Routes.Calendar,
                                    onNavigateTab = { route ->
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

                            // PROFILE ROUTE
                            composable(Routes.Profile) {
                                val profileViewModel: ProfileViewModel = viewModel(
                                    factory = ViewModelFactory(
                                        (applicationContext as PetCareApplication).userPreferencesRepository
                                    )
                                )
                                ProfileScreen(
                                    viewModel = profileViewModel,
                                    currentRoute = Routes.Profile,
                                    onNavigateTab = { route ->
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                        }
                                    },
                                    onNavigateToLogin = {
                                        navController.navigate(Routes.SignIn) {
                                            popUpTo(0)
                                        }
                                    }
                                )
                            }

                            // SIGN IN ROUTE
                            composable(Routes.SignIn) {
                                SignInScreen(
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
                                    onSignUpSuccess = {
                                        navController.navigate(Routes.Home) {
                                            popUpTo(Routes.SignUp) { inclusive = true }
                                        }
                                    },
                                    onGoToSignIn = { navController.popBackStack() }
                                )
                            }

                            // PET PROFILE ROUTE (con argumentos)
                            composable(
                                route = Routes.PetProfile,
                                arguments = listOf(navArgument("petId") { type = NavType.StringType })
                            ) { entry ->
                                val petId = entry.arguments?.getString("petId").orEmpty()
                                PetProfileScreen(
                                    petId = petId,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // NFC SCAN ROUTE
                            composable(Routes.NfcScan) {
                                ScanNFCScreen(
                                    onBack = { navController.popBackStack() },
                                    onDone = { navController.navigate(Routes.Home) }
                                )
                            }

                            // NFC WRITE ROUTE
                            composable(Routes.NfcWrite) {
                                WriteNFCScreen(
                                    onBack = { navController.popBackStack() },
                                    onDone = { navController.navigate(Routes.Home) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}