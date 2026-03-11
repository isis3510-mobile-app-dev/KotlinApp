package com.example.petcare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.theme.GrayMedium
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.OffWhite

@Composable
fun ProviderInfoCard(
    veterinarian: String,
    clinic: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(24.dp)
    ) {
        Text(
            text = "PROVIDER INFORMATION",
            style = MaterialTheme.typography.labelMedium,
            color = GrayText,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Veterinarian Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.PersonOutline,
                contentDescription = "Veterinarian",
                tint = GrayText,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Veterinarian", style = MaterialTheme.typography.bodySmall, color = GrayText)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = veterinarian,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = GrayMedium, thickness = 1.dp)
        
        // Clinic Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = "Clinic",
                tint = GrayText,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Clinic", style = MaterialTheme.typography.bodySmall, color = GrayText)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = clinic,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun ProviderInfoCardPreview() {
    PetCareTheme {
        Box(modifier = Modifier.background(OffWhite).padding(16.dp)) {
            ProviderInfoCard(
                veterinarian = "Dr. Smith",
                clinic = "Happy Paws Clinic"
            )
        }
    }
}
