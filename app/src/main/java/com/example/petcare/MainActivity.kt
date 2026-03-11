package com.example.petcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.data.preferences.AppThemeViewModel
import com.example.petcare.ui.screens.home.HomeScreen
import com.example.petcare.ui.screens.profile.ProfileScreen
import com.example.petcare.ui.screens.profile.ProfileViewModel
import com.example.petcare.ui.theme.LocalAppThemeMode
import com.example.petcare.ui.theme.PetCareTheme
import androidx.lifecycle.viewmodel.compose.viewModel

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

            CompositionLocalProvider(LocalAppThemeMode provides themeMode) {
                PetCareTheme(themeMode = themeMode) {
                    
                    // Simple state-based navigation for demonstration
                    var currentScreen by remember { mutableStateOf("profile") }
                    
                    when (currentScreen) {
                        "home" -> {
                            HomeScreen() // Assumes HomeScreen has its own NavBar or we update it later
                        }
                        "profile" -> {
                            val profileViewModel: ProfileViewModel = viewModel(
                                factory = ViewModelFactory((applicationContext as PetCareApplication).userPreferencesRepository)
                            )
                            ProfileScreen(
                                viewModel = profileViewModel,
                                onNavigateToLogin = { /* Handle Login */ },
                                onNavigateTab = { currentScreen = it }
                            )
                        }
                        else -> {
                            // Fallback
                            HomeScreen()
                        }
                    }
                }
            }
        }
    }
}