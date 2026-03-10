package com.example.petcare.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.RobotoMedium
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GrayBorder

@Composable
fun TextFieldComponent(name: String, label: String = "",icon: (@Composable () -> Unit)? = null) {

    var text by remember { mutableStateOf("") }
    PetCareTheme {
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
                onValueChange = { text = it },
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
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontFamily = RobotoMedium,
                    fontWeight = FontWeight.Normal
                ),
                colors = OutlinedTextFieldDefaults.colors(

                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = GrayBorder,

                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Gray
                ),
                trailingIcon = {
                    if (icon != null) {
                        IconButton(onClick = {}) {
                            icon()
                        }
                    }
                }

            )
        }
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
                tint = Color.LightGray
            )
            }
        )
    }
}