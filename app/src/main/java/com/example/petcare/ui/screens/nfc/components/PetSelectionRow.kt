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
import androidx.compose.ui.unit.dp
import com.example.petcare.R
import com.example.petcare.ui.theme.GrayDark
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme
import android.net.Uri
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.request.ImageRequest

data class PetOption(val id: String, val name: String, val photoUrl: String? = null)

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
            color = MaterialTheme.colorScheme.tertiary,
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
                        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.secondary) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        val context = LocalContext.current

                        if (pet.photoUrl.isNullOrBlank()) {
                            Image(
                                painter = painterResource(R.drawable.pet),
                                contentDescription = pet.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(Uri.parse(pet.photoUrl))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = pet.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                error = painterResource(R.drawable.pet)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = pet.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 72.dp)
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
        PetOption("1", "Max"),   // sin foto → muestra drawable
        PetOption("2", "Luna"),
        PetOption("3", "Coco")
    )
    PetCareTheme {
        PetSelectionRow(
            pets = samplePets,
            selectedPetId = "1",
            onPetSelected = {}
        )
    }
}