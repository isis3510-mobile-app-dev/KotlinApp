package com.example.petcare.ui.screens.profile.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.contentDescription
import com.example.petcare.ui.components.SettingsListItem
import com.example.petcare.ui.components.SettingsSection
import com.example.petcare.ui.theme.*

@Composable
fun AccountSection(
    onEditProfileClick: () -> Unit,
    onEmailClick: () -> Unit,
    onPhoneClick: () -> Unit
) {
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
                onClick = onEditProfileClick
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
                onClick = onEmailClick
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
                onClick = onPhoneClick
            )
        }
    ))
}
