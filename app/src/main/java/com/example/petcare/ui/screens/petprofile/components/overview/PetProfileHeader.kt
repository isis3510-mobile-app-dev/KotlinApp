package com.example.petcare.ui.screens.petprofile.components.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.R
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.GreenLight
import com.example.petcare.ui.theme.PetCareTheme

@Composable
fun PetProfileHeader(
    name: String,
    breed: String,
    species: String,
    age: String,
    weight: String,
    gender: String,
    isHealthy: Boolean = true,
    isNfcSynched: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(GreenDark)
            .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pet Avatar with optional NFC Badge
            Box(contentAlignment = Alignment.BottomEnd) {
                // Main Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.pet),
                        contentDescription = "Pet Avatar",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // NFC Badge
                if (isNfcSynched) {
                    Box(
                        modifier = Modifier
                            .offset(x = 8.dp, y = 8.dp) // Push out past the corner slightly
                            .size(32.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(GreenLight)
                            .border(2.dp, GreenDark, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Contactless,
                            contentDescription = "NFC Synched",
                            tint = GreenDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        fontSize = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    if (isHealthy) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(GreenLight, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ Healthy",
                                color = GreenDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "$breed · $species",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Age
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎂", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = age, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    
                    // Weight
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚖️", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = weight, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }

                    // Gender
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val genderEmoji = if (gender.contains("Male", true)) "♂" else "♀"
                        Text(text = genderEmoji, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = gender, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PetProfileHeaderPreview() {
    PetCareTheme {
        PetProfileHeader(
            name = "Max",
            breed = "Golden Retriever",
            species = "Dog",
            age = "6 yrs",
            weight = "28.5 kg",
            gender = "Male",
            isNfcSynched = true
        )
    }
}
