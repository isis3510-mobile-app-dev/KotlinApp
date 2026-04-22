package com.example.petcare.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.containsOnlyWhitespace
import com.example.petcare.util.normalizeForCommit
import com.example.petcare.util.sanitizeForEditing
import com.example.petcare.util.trimToNullIfBlank
import com.example.petcare.util.validateCommittedInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFieldBottomSheet(
    title: String,
    currentValue: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int? = null,
    fieldPolicy: InputFieldPolicy = InputFieldPolicy.GENERAL_TEXT,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(currentValue) { mutableStateOf(currentValue) }
    var errorMessage by remember(currentValue) { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun saveIfValid() {
        val validationMessage = validateCommittedInput(
            value = value,
            fieldPolicy = fieldPolicy,
            required = true,
            maxLength = maxLength
        )
        if (validationMessage != null) {
            errorMessage = validationMessage
            return
        }

        onSave(normalizeForCommit(value, fieldPolicy))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = value,
                onValueChange = {
                    val sanitized = sanitizeForEditing(
                        raw = it,
                        fieldPolicy = fieldPolicy,
                        maxLength = maxLength
                    )
                    value = sanitized.value
                    errorMessage = sanitized.rejectionMessage ?: if (containsOnlyWhitespace(sanitized.value)) {
                        "Only spaces are not allowed."
                    } else {
                        null
                    }
                },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { saveIfValid() }
                ),
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { saveIfValid() },
                modifier = Modifier.fillMaxWidth(),
                enabled = value.trimToNullIfBlank() != null &&
                    normalizeForCommit(value, fieldPolicy) != normalizeForCommit(currentValue, fieldPolicy)
            ) {
                Text("Save")
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
