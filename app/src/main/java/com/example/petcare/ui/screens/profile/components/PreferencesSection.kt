package com.example.petcare.ui.screens.profile.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.petcare.data.preferences.AppThemeMode
import com.example.petcare.ui.components.SettingsListItem
import com.example.petcare.ui.components.SettingsSection
import com.example.petcare.ui.theme.*

@Composable
fun PreferencesSection(
    currentThemeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsToggled: (Boolean) -> Unit,
    offlineModeEnabled: Boolean,
    onOfflineModeToggled: (Boolean) -> Unit
) {
    SettingsSection(title = "Preferences", items = listOf(
        {
            SettingsListItem(
                icon = Icons.Default.DarkMode,
                iconBackgroundColor = Color.LightGray.copy(alpha = 0.2f),
                iconTintColor = Color.DarkGray,
                title = "Dark Mode",
                trailingContent = {
                    Switch(
                        checked = currentThemeMode == AppThemeMode.DARK,
                        onCheckedChange = { isDark ->
                            val mode = if (isDark) AppThemeMode.DARK else AppThemeMode.LIGHT
                            onThemeModeChanged(mode)
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
                subtitle = if (notificationsEnabled) "Enabled" else "Disabled",
                trailingContent = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onNotificationsToggled,
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
                        checked = offlineModeEnabled,
                        onCheckedChange = onOfflineModeToggled,
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
}
