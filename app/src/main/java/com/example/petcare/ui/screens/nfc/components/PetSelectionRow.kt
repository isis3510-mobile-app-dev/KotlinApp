package com.example.petcare.ui.screens.nfc.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.R
import com.example.petcare.ui.theme.GrayDark
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme

data class PetOption(val id: String, val name: String, val imageRes: Int)

@Composable
fun PetSelectionRow(
    pets: List<PetOption>,
    selectedPetId: String?,
    onPetSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Select pet to write:",
            style = MaterialTheme.typography.titleMedium,
            color = GrayDark,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp)
        ) {
            items(pets) { pet ->
                val isSelected = pet.id == selectedPetId
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onPetSelected(pet.id) }
                ) {
                    Card(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = if (isSelected) BorderStroke(3.dp, GreenDark) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Image(
                            painter = painterResource(id = pet.imageRes),
                            contentDescription = pet.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = pet.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) GreenDark else GrayDark,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PetSelectionRowPreview() {
    val samplePets = listOf(
        PetOption("1", "Max", R.drawable.pet),
        PetOption("2", "Luna", R.drawable.pet),
        PetOption("3", "Coco", R.drawable.pet)
    )
    
    PetCareTheme {
        PetSelectionRow(
            pets = samplePets,
            selectedPetId = "1",
            onPetSelected = {}
        )
    }
}
