package com.example.petcare.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.RobotoMedium
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GrayBorder
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.containsOnlyWhitespace
import com.example.petcare.util.enforceMaxLength
import com.example.petcare.util.sanitizeForEditing
import com.example.petcare.util.validateLiveInput

@Composable
fun TextFieldComponent(name: String, label: String = "", icon: (@Composable () -> Unit)? = null,
                       value: String? = null,
                       onValueChange: ((String) -> Unit)? = null,
                       visualTransformation: VisualTransformation = VisualTransformation.None,
                       keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
                       maxLength: Int? = null,
                       fieldPolicy: InputFieldPolicy = InputFieldPolicy.GENERAL_TEXT,
                       maxNumericValue: Double? = null,
                       isError: Boolean = false,
                       supportingText: String? = null
){
    var internalText by remember{ mutableStateOf("")}
    var localError by remember(name, fieldPolicy, maxLength, maxNumericValue) {
        mutableStateOf<String?>(null)
    }
    val text = enforceMaxLength(value ?: internalText, maxLength)
    val validationMessage = supportingText
        ?: localError
        ?: when {
            containsOnlyWhitespace(text) -> "Only spaces are not allowed."
            else -> validateLiveInput(
                value = text,
                fieldPolicy = fieldPolicy,
                maxNumericValue = maxNumericValue
            )
        }
    Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = text,
                onValueChange = {
                    newValue ->
                    val sanitized = sanitizeForEditing(
                        raw = newValue,
                        fieldPolicy = fieldPolicy,
                        maxLength = maxLength,
                        maxNumericValue = maxNumericValue
                    )
                    localError = sanitized.rejectionMessage
                    if (onValueChange != null){
                        onValueChange(sanitized.value)
                    } else {
                        internalText = sanitized.value
                    }
                },
                modifier = Modifier
                    .size(width = 342.dp, height = 53.17.dp)
                    .clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                placeholder = {
                    Text(
                        label,
                        color = GrayText
                    )
                },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontFamily = RobotoMedium,
                    fontWeight = FontWeight.Normal
                ),
                colors = OutlinedTextFieldDefaults.colors(

                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = GrayBorder,

                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                isError = isError || validationMessage != null,
                trailingIcon = {
                    if (icon != null) {
                        IconButton(onClick = {}) {
                            icon()
                        }
                    }
                },
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                supportingText = validationMessage?.let {
                    { Text(text = it, color = MaterialTheme.colorScheme.error) }
                }

            )
        }
    }


@Preview(showBackground = true)
@Composable
fun TextFieldPreview(){
    PetCareTheme{
        TextFieldComponent(
            name = "Full Name",
            label = "Sarah Johnson",
            icon = { Icon(
                imageVector = Icons.Default.RemoveRedEye,
                contentDescription = "Call Owner Now",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            }
        )
    }
}
