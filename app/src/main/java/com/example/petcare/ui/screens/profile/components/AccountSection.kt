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
import com.example.petcare.ui.components.EditEmailBottomSheet
import com.example.petcare.ui.components.EditFieldBottomSheet
import com.example.petcare.ui.components.SettingsListItem
import com.example.petcare.ui.components.SettingsSection
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GreenLight
import com.example.petcare.ui.theme.InfoContainer
import com.example.petcare.ui.theme.InfoContent
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.SuccessContent
import com.example.petcare.util.DisplayTextLimits
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.truncateForDisplay


private enum class ActiveSheet { NONE, NAME, PHONE, EMAIL }

@Composable
fun AccountSection(
    userName: String = "---",
    userEmail: String = "---",
    userPhone: String = "",
    onSaveName: (String) -> Unit,
    onSavePhone: (String) -> Unit,

    onSaveEmail: (newEmail: String, currentPassword: String) -> Unit,
    isLoading: Boolean = false,
) {
    var activeSheet by remember { mutableStateOf(ActiveSheet.NONE) }

    SettingsSection(title = "Account", items = listOf(
        {
            SettingsListItem(
                icon = Icons.Default.Person,
                iconBackgroundColor = GreenLight.copy(alpha = 0.2f),
                iconTintColor = MaterialTheme.colorScheme.secondary,
                title = "Edit Profile",
                subtitle = userName.truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.onTertiary
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
                subtitle = userEmail.truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit Email",
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                },
                onClick = {activeSheet = ActiveSheet.EMAIL}
            )
        },
        {
            SettingsListItem(
                icon = Icons.Default.Phone,
                iconBackgroundColor = SuccessContainer,
                iconTintColor = SuccessContent,
                title = "Phone",
                subtitle = userPhone.ifEmpty { "Add phone number" }
                    .truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit Phone",
                        tint = MaterialTheme.colorScheme.onTertiary
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
            maxLength = InputTextLimits.USER_NAME,
            fieldPolicy = InputFieldPolicy.GENERAL_TEXT,
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
            maxLength = InputTextLimits.PHONE,
            fieldPolicy = InputFieldPolicy.PHONE,
            onDismiss = { activeSheet = ActiveSheet.NONE },
            onSave = {
                onSavePhone(it)
                activeSheet = ActiveSheet.NONE
            }
        )
        ActiveSheet.EMAIL -> EditEmailBottomSheet(
            currentEmail = userEmail,
            isLoading = isLoading,
            onDismiss = { activeSheet = ActiveSheet.NONE },
            onSave = { newEmail, password ->
                onSaveEmail(newEmail, password)
                activeSheet = ActiveSheet.NONE
            }
        )
        ActiveSheet.NONE -> Unit
    }
}
