package com.example.petcare.ui.screens.petprofile.components.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.OffWhite

@Composable
fun PetInformationCard(
    species: String,
    breed: String,
    dateOfBirth: String,
    age: String,
    weight: String,
    color: String,
    gender: String,
    microchip: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "PET INFORMATION",
                style = MaterialTheme.typography.labelMedium,
                color = GrayText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 2-Column Grid
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoItem(label = "Species", value = species, modifier = Modifier.weight(1f))
                    InfoItem(label = "Breed", value = breed, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoItem(label = "Date of Birth", value = dateOfBirth, modifier = Modifier.weight(1f))
                    InfoItem(label = "Age", value = age, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoItem(label = "Weight", value = weight, modifier = Modifier.weight(1f))
                    InfoItem(label = "Color", value = color, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoItem(label = "Gender", value = gender, modifier = Modifier.weight(1f))
                    InfoItem(label = "Microchip", value = microchip, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = GrayText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = false)
@Composable
fun PetInformationCardPreview() {
    PetCareTheme {
        Box(modifier = Modifier.padding(16.dp).background(OffWhite)) {
            PetInformationCard(
                species = "Dog",
                breed = "Golden Retriever",
                dateOfBirth = "Mar 14, 2020",
                age = "6 yrs",
                weight = "28.5 kg",
                color = "Golden",
                gender = "Male",
                microchip = "XR123456789"
            )
        }
    }
}
