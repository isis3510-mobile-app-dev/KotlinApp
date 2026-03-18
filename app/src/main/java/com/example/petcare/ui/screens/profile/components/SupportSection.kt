package com.example.petcare.ui.screens.profile.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.petcare.ui.components.SettingsListItem
import com.example.petcare.ui.components.SettingsSection

@Composable
fun SupportSection(
    onSignOutClicked: () -> Unit,
    onDeleteAccountClicked: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

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
        },
        {
            SettingsListItem(
                icon = Icons.Default.DeleteForever,
                iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                iconTintColor = MaterialTheme.colorScheme.error,
                title = "Delete Account",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { showDeleteDialog = true }
            )
        }
    ))

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete account") },
            text = {
                Text(
                    "This action is permanent and cannot be undone. " +
                            "All your data and pets will be deleted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccountClicked()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}