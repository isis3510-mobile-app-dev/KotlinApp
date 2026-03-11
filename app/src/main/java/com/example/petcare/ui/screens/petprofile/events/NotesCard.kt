package com.example.petcare.ui.screens.petprofile.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
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
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.PetCareTheme
import com.example.petcare.ui.theme.OffWhite

@Composable
fun NotesCard(
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "NOTES",
                style = MaterialTheme.typography.labelMedium,
                color = GrayText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                lineHeight = 24.sp
            )
        }
        
        // Teal filled FAB anchored in the bottom right contextually or perhaps top right
        // The mockup shows it bleeding slightly out near the top right of the section,
        // but for a clean layout we can anchor it strictly inner-aligned, top-right padding.
    }
}

@Preview(showBackground = false)
@Composable
fun NotesCardPreview() {
    PetCareTheme {
        Box(modifier = Modifier.background(OffWhite).padding(16.dp)) {
            NotesCard(
                description = "Annual wellness exam. All vitals normal. Weight stable at 28.5kg."
            )
        }
    }
}
