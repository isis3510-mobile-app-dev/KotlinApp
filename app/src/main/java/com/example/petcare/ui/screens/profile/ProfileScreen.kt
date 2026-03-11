package com.example.petcare.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.data.preferences.AppThemeMode
import com.example.petcare.ui.components.NavBar
import com.example.petcare.ui.components.ProfileHeader
import com.example.petcare.ui.components.SettingsListItem
import com.example.petcare.ui.components.SettingsSection
import com.example.petcare.ui.components.ExpandableFAB
import com.example.petcare.ui.screens.profile.components.AccountSection
import com.example.petcare.ui.screens.profile.components.PreferencesSection
import com.example.petcare.ui.screens.profile.components.SupportSection
import com.example.petcare.ui.theme.InfoContainer
import com.example.petcare.ui.theme.InfoContent
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.SuccessContent
import com.example.petcare.ui.theme.WarningContainer
import com.example.petcare.ui.theme.WarningContent

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToLogin: () -> Unit = {},
    onNavigateTab: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExpandableFAB()
        },
        bottomBar = {
            NavBar(
                currentRoute = "profile",
                onItemClick = onNavigateTab
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileHeader(
                name = "Sarah Johnson",
                email = "sarah.johnson@email.com",
                petCount = 3,
                initials = "SJ" // Replace with actual user details logic
            )

            // Account Section
            AccountSection(
                onEditProfileClick = { /* Navigate to edit profile */ },
                onEmailClick = { /* Navigate to email */ },
                onPhoneClick = { /* Navigate to phone */ }
            )

            // Preferences Section
            PreferencesSection(
                currentThemeMode = uiState.currentThemeMode,
                onThemeModeChanged = viewModel::onThemeModeChanged,
                notificationsEnabled = uiState.notificationsEnabled,
                onNotificationsToggled = viewModel::onNotificationsToggled,
                offlineModeEnabled = uiState.offlineModeEnabled,
                onOfflineModeToggled = viewModel::onOfflineModeToggled
            )

            // Support Section
            SupportSection(
                onSignOutClicked = viewModel::onSignOutClicked
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// A Preview with mocked state
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreviewStub() {
    PetCareTheme {
        Scaffold(
            floatingActionButton = { ExpandableFAB() },
            bottomBar = { NavBar(currentRoute = "profile", onItemClick = {}) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ProfileHeader(
                    name = "Sarah Johnson",
                    email = "sarah.johnson@email.com",
                    petCount = 3,
                    initials = "SJ"
                )

                // Account Section
                AccountSection(
                    onEditProfileClick = {},
                    onEmailClick = {},
                    onPhoneClick = {}
                )

                // Preferences Section
                PreferencesSection(
                    currentThemeMode = AppThemeMode.LIGHT,
                    onThemeModeChanged = {},
                    notificationsEnabled = true,
                    onNotificationsToggled = {},
                    offlineModeEnabled = false,
                    onOfflineModeToggled = {}
                )

                // Support Section
                SupportSection(
                    onSignOutClicked = {}
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
