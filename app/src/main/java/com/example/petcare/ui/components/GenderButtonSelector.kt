package com.example.petcare.ui.components

import com.example.petcare.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

// 🔹 Modelo más limpio
data class GenderOption(
    val name: String,
    val icon: ImageVector
)

@Composable
fun GenderSelector(
    title: String = "Gender",
    options: List<GenderOption> = listOf(
        GenderOption("Male", Icons.Filled.Male),
        GenderOption("Female", Icons.Filled.Female)
    ),
    onOptionSelected: (String) -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = option.name == selectedOption

                val backgroundColor =
                    if (isSelected) GreenDark else MaterialTheme.colorScheme.surface

                val borderColor =
                    if (isSelected) GreenDark else Color.Gray

                val contentColor =
                    if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(
                            width = 2.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(30.dp)
                        )
                        .background(
                            color = backgroundColor,
                            shape = RoundedCornerShape(30.dp)
                        )
                        .clickable {
                            selectedOption = option.name
                            onOptionSelected(option.name)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = option.name,
                            tint = contentColor
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = option.name,
                            color = contentColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GenderSelectorPreview() {
    PetCareTheme {
        GenderSelector { selected ->
            println("Selected: $selected")
        }
    }
}