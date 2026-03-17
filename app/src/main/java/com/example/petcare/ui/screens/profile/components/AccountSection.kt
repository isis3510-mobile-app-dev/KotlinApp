package com.example.petcare.ui.screens.profile.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.example.petcare.ui.components.EditFieldBottomSheet
import com.example.petcare.ui.components.SettingsListItem
import com.example.petcare.ui.components.SettingsSection
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GreenLight
import com.example.petcare.ui.theme.InfoContainer
import com.example.petcare.ui.theme.InfoContent
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.SuccessContent


private enum class ActiveSheet { NONE, NAME, PHONE }

@Composable
fun AccountSection(
    userName: String = "---",
    userEmail: String = "---",
    userPhone: String = "",
    onSaveName: (String) -> Unit,
    onSavePhone: (String) -> Unit,

    onEmailClick: () -> Unit,
) {
    var activeSheet by remember { mutableStateOf(ActiveSheet.NONE) }

    SettingsSection(title = "Account", items = listOf(
        {
            SettingsListItem(
                icon = Icons.Default.Person,
                iconBackgroundColor = GreenLight.copy(alpha = 0.2f),
                iconTintColor = MaterialTheme.colorScheme.secondary,
                title = "Edit Profile",
                subtitle = userName,
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit Profile",
                        tint = GrayText
                    )
                },
                onClick = { activeSheet = ActiveSheet.NAME }
            )
        },
        {
            SettingsListItem(
                icon = Icons.Default.Email,
                iconBackgroundColor = InfoContainer,
                iconTintColor = InfoContent,
                title = "Email",
                subtitle = userEmail,
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit Email",
                        tint = GrayText
                    )
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
                subtitle = userPhone.ifEmpty { "Add phone number" },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit Phone",
                        tint = GrayText
                    )
                },
                onClick = { activeSheet = ActiveSheet.PHONE }
            )
        }
    ))

    when (activeSheet) {
        ActiveSheet.NAME -> EditFieldBottomSheet(
            title = "Edit name",
            currentValue = userName,
            placeholder = "Full name",
            onDismiss = { activeSheet = ActiveSheet.NONE },
            onSave = {
                onSaveName(it)
                activeSheet = ActiveSheet.NONE
            }
        )
        ActiveSheet.PHONE -> EditFieldBottomSheet(
            title = "Edit phone",
            currentValue = userPhone,
            placeholder = "+57 300 123 4567",
            keyboardType = KeyboardType.Phone,
            onDismiss = { activeSheet = ActiveSheet.NONE },
            onSave = {
                onSavePhone(it)
                activeSheet = ActiveSheet.NONE
            }
        )
        ActiveSheet.NONE -> Unit
    }
}