package com.example.petcare.ui.screens.profile.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.petcare.ui.components.SettingsListItem
import com.example.petcare.ui.components.SettingsSection

@Composable
fun SupportSection(
    onSignOutClicked: () -> Unit
) {
    SettingsSection(title = "", items = listOf(
        {
            SettingsListItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                iconTintColor = MaterialTheme.colorScheme.error,
                title = "Sign Out",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = onSignOutClicked
            )
        }
    ))
}
