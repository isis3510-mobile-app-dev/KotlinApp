package com.example.petcare.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.data.preferences.AppThemeMode
import com.example.petcare.ui.components.ExpandableFAB
import com.example.petcare.ui.components.NavBar
import com.example.petcare.ui.components.ProfileHeader
import com.example.petcare.ui.screens.profile.components.AccountSection
import com.example.petcare.ui.screens.profile.components.PreferencesSection
import com.example.petcare.ui.screens.profile.components.SupportSection
import com.example.petcare.ui.theme.PetCareTheme


@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    petCount: Int? = null,
    onNavigateToLogin: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp)

) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.NavigateToLogin -> onNavigateToLogin()
                is UiEvent.SaveSuccess -> snackbarHostState.showSnackbar("Saved successfully")
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileHeader(
                name = uiState.user?.name ?: "---",
                email = uiState.user?.email ?: "---",
                petCount = petCount ?: uiState.user?.pets?.size ?: 0,
                initials = uiState.user?.initials ?: "?"
            )

            // Account Section
            AccountSection(
                userName = uiState.user?.name ?: "---",
                userEmail = uiState.user?.email ?: "---",
                userPhone = uiState.user?.phone ?: "",
                onSaveName = { viewModel.updateField(ProfileViewModel.EditField.Name, it) },
                onSavePhone = { viewModel.updateField(ProfileViewModel.EditField.Phone, it) },
                onSaveEmail = { newEmail, password -> viewModel.updateEmail(password, newEmail) }
            )

            // Preferences Section
            PreferencesSection(
                currentThemeMode = uiState.currentThemeMode,
                onThemeModeChanged = viewModel::onThemeModeChanged,
                notificationsEnabled = uiState.notificationsEnabled,
                onNotificationsToggled = viewModel::onNotificationsToggled,
                //offlineModeEnabled = uiState.offlineModeEnabled,
                //onOfflineModeToggled = viewModel::onOfflineModeToggled
            )

            // Support Section
            SupportSection(
                onSignOutClicked = viewModel::onSignOutClicked,
                onDeleteAccountClicked = viewModel::deleteAccount
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

}

// A Preview with mocked state
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreviewStub() {
    PetCareTheme {
        Scaffold(
            floatingActionButton = { ExpandableFAB(
                onAddPet = {},
                onAddEvent = {},
                onAddVaccine = {},
                onScanNFC = {}
            ) },
            bottomBar = { NavBar(currentRoute = "profile", onItemClick = {}) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.systemBars),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ProfileHeader(
                    name = "Sarah Johnson",
                    email = "sarah.johnson@email.com",
                    petCount = 3,
                    initials = "SJ"
                )

                // Account Section
                //AccountSection(
                //    onEditProfileClick = {},
                //    onEmailClick = {},
                //    onPhoneClick = {} )

                // Preferences Section
                PreferencesSection(
                    currentThemeMode = AppThemeMode.LIGHT,
                    onThemeModeChanged = {},
                    notificationsEnabled = true,
                    onNotificationsToggled = {},
                    //offlineModeEnabled = false,
                    //onOfflineModeToggled = {}
                )

                // Support Section
                SupportSection(
                    onSignOutClicked = {},
                    onDeleteAccountClicked = {}
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
