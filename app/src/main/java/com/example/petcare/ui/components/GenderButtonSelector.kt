package com.example.petcare.ui.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

data class GenderOption(val name: String, val icon: @Composable () -> Unit)

@Composable
fun GenderSelector(
    title: String = "Species",
    options: List<GenderOption> = listOf(
        GenderOption("Male") { Icon(Icons.Filled.Male, contentDescription = "Male", tint = Color.Black) },
        GenderOption("Female") { Icon(Icons.Filled.Female, contentDescription = "Female", tint = Color.Black) }
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
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = option.name == selectedOption

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) Color(0xFF006A60) else Color.Gray,
                            shape = RoundedCornerShape(30.dp)
                        )
                        .background(
                            color = if (isSelected) Color(0xFF006A60) else Color.White,
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

                        Box(modifier = Modifier.size(20.dp)) {
                            option.icon()
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = option.name,
                            color = if (isSelected) Color.White else Color.Black,
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
    GenderSelector { selected ->
        println("Selected: $selected")
    }
}