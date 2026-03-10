package com.example.petcare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.petcare.R
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.GreenDark

data class SpeciesOption(val name: String, val iconResId: Int)

@Composable
fun SpeciesSelector(
    title: String = "Species *",
    options: List<SpeciesOption> = listOf(
        SpeciesOption("Dog", R.drawable.dog_logo),
        SpeciesOption("Cat", R.drawable.cat_logo)
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
                        .size(width = 111.33.dp, height = 42.67.dp)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) GreenDark else Color.LightGray,
                            shape = RoundedCornerShape(30.dp)
                        )
                        .background(
                            color = if (isSelected) GreenDark else Color.White,
                            shape = RoundedCornerShape(12.dp)
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

                        Image(
                            painter = painterResource(id = option.iconResId),
                            contentDescription = option.name,
                            modifier = Modifier.size(24.dp)
                        )

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
fun SpeciesSelectorPreview() {
    PetCareTheme {
        SpeciesSelector { selected ->
            println("Selected: $selected")
        }
    }
}