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
import com.example.petcare.ui.theme.GrayBorder
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GreenLight
import com.example.petcare.ui.theme.GreenDark
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
            SettingsSection(title = "Account", items = listOf(
                {
                    SettingsListItem(
                        icon = Icons.Default.Person,
                        iconBackgroundColor = GreenLight.copy(alpha = 0.2f),
                        iconTintColor = MaterialTheme.colorScheme.secondary,
                        title = "Edit Profile",
                        subtitle = "Sarah Johnson",
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Edit Profile", tint = GrayText)
                        },
                        onClick = { /* Navigate to edit profile */ }
                    )
                },
                {
                    SettingsListItem(
                        icon = Icons.Default.Email,
                        iconBackgroundColor = InfoContainer,
                        iconTintColor = InfoContent,
                        title = "Email",
                        subtitle = "sarah.johnson@email.com",
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Edit Email", tint = GrayText)
                        },
                        onClick = { /* Navigate to email */ }
                    )
                },
                {
                    SettingsListItem(
                        icon = Icons.Default.Phone,
                        iconBackgroundColor = SuccessContainer,
                        iconTintColor = SuccessContent,
                        title = "Phone",
                        subtitle = "+1 (555) 012-3456",
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Edit Phone", tint = GrayText)
                        },
                        onClick = { /* Navigate to phone */ }
                    )
                }
            ))

            // Preferences Section
            SettingsSection(title = "Preferences", items = listOf(
                {
                    SettingsListItem(
                        icon = Icons.Default.DarkMode,
                        iconBackgroundColor = Color.LightGray.copy(alpha = 0.2f),
                        iconTintColor = Color.DarkGray,
                        title = "Dark Mode",
                        trailingContent = {
                            Switch(
                                checked = uiState.currentThemeMode == AppThemeMode.DARK,
                                onCheckedChange = { isDark ->
                                    val mode = if (isDark) AppThemeMode.DARK else AppThemeMode.LIGHT
                                    viewModel.onThemeModeChanged(mode)
                                },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedTrackColor = GreenDark,
                                    uncheckedTrackColor = GrayBorder,
                                    checkedThumbColor = Color.White
                                ),
                                modifier = Modifier.semantics { contentDescription = "Toggle Dark Mode" }
                            )
                        }
                    )
                },
                {
                    SettingsListItem(
                        icon = Icons.Default.Notifications,
                        iconBackgroundColor = WarningContainer,
                        iconTintColor = WarningContent,
                        title = "Notifications",
                        subtitle = if (uiState.notificationsEnabled) "Enabled" else "Disabled",
                        trailingContent = {
                            Switch(
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.onNotificationsToggled(it) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedTrackColor = GreenDark,
                                    uncheckedTrackColor = GrayBorder,
                                    checkedThumbColor = Color.White
                                ),
                                modifier = Modifier.semantics { contentDescription = "Toggle Notifications" }
                            )
                        }
                    )
                },
                {
                    SettingsListItem(
                        icon = Icons.Default.WifiOff,
                        iconBackgroundColor = Color.LightGray.copy(alpha = 0.2f),
                        iconTintColor = Color.DarkGray,
                        title = "Offline Mode",
                        trailingContent = {
                            Switch(
                                checked = uiState.offlineModeEnabled,
                                onCheckedChange = { viewModel.onOfflineModeToggled(it) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedTrackColor = GreenDark,
                                    uncheckedTrackColor = GrayBorder,
                                    checkedThumbColor = Color.White
                                ),
                                modifier = Modifier.semantics { contentDescription = "Toggle Offline Mode" }
                            )
                        }
                    )
                }
            ))

            // Support Section
            SettingsSection(title = "Support", items = listOf(
                {
                    SettingsListItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                        iconTintColor = MaterialTheme.colorScheme.error,
                        title = "Sign Out",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.onSignOutClicked() }
                    )
                }
            ))

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
                SettingsSection(title = "Account", items = listOf(
                    {
                        SettingsListItem(
                            icon = Icons.Default.Person,
                            iconBackgroundColor = GreenLight.copy(alpha = 0.2f),
                            iconTintColor = MaterialTheme.colorScheme.secondary,
                            title = "Edit Profile",
                            subtitle = "Sarah Johnson",
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Edit Profile", tint = GrayText)
                            },
                        )
                    },
                    {
                        SettingsListItem(
                            icon = Icons.Default.Email,
                            iconBackgroundColor = InfoContainer,
                            iconTintColor = InfoContent,
                            title = "Email",
                            subtitle = "sarah.johnson@email.com",
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Edit Email", tint = GrayText)
                            },
                        )
                    },
                    {
                        SettingsListItem(
                            icon = Icons.Default.Phone,
                            iconBackgroundColor = SuccessContainer,
                            iconTintColor = SuccessContent,
                            title = "Phone",
                            subtitle = "+1 (555) 012-3456",
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Edit Phone", tint = GrayText)
                            },
                        )
                    }
                ))

                // Preferences Section
                SettingsSection(title = "Preferences", items = listOf(
                    {
                        SettingsListItem(
                            icon = Icons.Default.DarkMode,
                            iconBackgroundColor = Color.LightGray.copy(alpha = 0.2f),
                            iconTintColor = Color.DarkGray,
                            title = "Dark Mode",
                            trailingContent = {
                                Switch(
                                    checked = false, 
                                    onCheckedChange = {},
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedTrackColor = GreenDark,
                                        uncheckedTrackColor = GrayBorder,
                                        checkedThumbColor = Color.White
                                    )
                                )
                            }
                        )
                    },
                    {
                        SettingsListItem(
                            icon = Icons.Default.Notifications,
                            iconBackgroundColor = WarningContainer,
                            iconTintColor = WarningContent,
                            title = "Notifications",
                            subtitle = "Enabled",
                            trailingContent = {
                                Switch(
                                    checked = true, 
                                    onCheckedChange = {},
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedTrackColor = GreenDark,
                                        uncheckedTrackColor = GrayBorder,
                                        checkedThumbColor = Color.White
                                    )
                                )
                            }
                        )
                    },
                    {
                        SettingsListItem(
                            icon = Icons.Default.WifiOff,
                            iconBackgroundColor = Color.LightGray.copy(alpha = 0.2f),
                            iconTintColor = Color.DarkGray,
                            title = "Offline Mode",
                            trailingContent = {
                                Switch(
                                    checked = false, 
                                    onCheckedChange = {},
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedTrackColor = GreenDark,
                                        uncheckedTrackColor = GrayBorder,
                                        checkedThumbColor = Color.White
                                    )
                                )
                            }
                        )
                    }
                ))

                // Support Section
                SettingsSection(title = "", items = listOf(
                    {
                        SettingsListItem(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                            iconTintColor = MaterialTheme.colorScheme.error,
                            title = "Sign Out",
                            titleColor = MaterialTheme.colorScheme.error
                        )
                    }
                ))

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
