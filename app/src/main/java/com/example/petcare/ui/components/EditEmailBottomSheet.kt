package com.example.petcare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits
import com.example.petcare.util.containsOnlyWhitespace
import com.example.petcare.util.normalizeForCommit
import com.example.petcare.util.sanitizeForEditing
import com.example.petcare.util.trimToNullIfBlank
import com.example.petcare.util.validateCommittedInput


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEmailBottomSheet(
    currentEmail: String,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (newEmail: String, currentPassword: String) -> Unit
) {
    var newEmail by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun submitIfValid() {
        val normalizedEmail = normalizeForCommit(newEmail, InputFieldPolicy.EMAIL)
        val normalizedPassword = normalizeForCommit(currentPassword, InputFieldPolicy.PASSWORD)

        emailError = validateCommittedInput(
            value = newEmail,
            fieldPolicy = InputFieldPolicy.EMAIL,
            required = true,
            maxLength = InputTextLimits.EMAIL,
            fieldName = "Email"
        )
        passwordError = validateCommittedInput(
            value = currentPassword,
            fieldPolicy = InputFieldPolicy.PASSWORD,
            required = true,
            maxLength = InputTextLimits.PASSWORD,
            fieldName = "Password"
        )

        if (emailError == null && passwordError == null) {
            onSave(normalizedEmail, normalizedPassword)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Update email",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Current: $currentEmail",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Nuevo email
            OutlinedTextField(
                value = newEmail,
                onValueChange = {
                    val sanitized = sanitizeForEditing(
                        raw = it,
                        fieldPolicy = InputFieldPolicy.EMAIL,
                        maxLength = InputTextLimits.EMAIL
                    )
                    newEmail = sanitized.value
                    emailError = sanitized.rejectionMessage ?: if (containsOnlyWhitespace(sanitized.value)) {
                        "Only spaces are not allowed."
                    } else {
                        null
                    }
                },
                label = { Text("New email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it) } },

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    cursorColor = MaterialTheme.colorScheme.secondary
                )
            )

            OutlinedTextField(
                value = currentPassword,
                onValueChange = {
                    val sanitized = sanitizeForEditing(
                        raw = it,
                        fieldPolicy = InputFieldPolicy.PASSWORD,
                        maxLength = InputTextLimits.PASSWORD
                    )
                    currentPassword = sanitized.value
                    passwordError = sanitized.rejectionMessage ?: if (containsOnlyWhitespace(sanitized.value)) {
                        "Only spaces are not allowed."
                    } else {
                        null
                    }
                },
                label = { Text("Current password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Default.VisibilityOff
                            else
                                Icons.Default.Visibility,
                            contentDescription = "Toggle password"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submitIfValid() }
                ),
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    cursorColor = MaterialTheme.colorScheme.secondary
                )
            )

            Text(
                text = "You will receive a confirmation email on the new email. Please verify to complete the change. You will need to Sign In again once this operation is completed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { submitIfValid() },
                modifier = Modifier.fillMaxWidth(),
                enabled = newEmail.trimToNullIfBlank() != null &&
                        currentPassword.trimToNullIfBlank() != null &&
                        normalizeForCommit(newEmail, InputFieldPolicy.EMAIL) != currentEmail &&
                        !isLoading,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    Text("Update email")
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Cancel")
            }
        }
    }
}
