package com.example.petcare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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

@Composable
fun NextEventCard(name: String, pet: String, maxDate: String, daysOverdue: Int){
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFB2EBE0)),
                contentAlignment = Alignment.Center
            ){
                Icon(
                    imageVector = Icons.Default.Vaccines,
                    contentDescription = "Vaccine",
                    tint = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NEXT VACCINE",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    Text (
                        text = "$pet - ",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Text (
                        text = maxDate,
                        fontSize = 14.sp,
                        color = Color.Gray
                )
                }
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(Color(0xFFE3F2FD)).padding(horizontal = 10.dp, vertical = 4.dp)
            ){
                Text(
                    text = "-${daysOverdue}d",
                    fontSize = 12.sp,
                    color = Color(0xFF1565C0),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun NextEventCardPreview(){
    NextEventCard(name="Rabies",
        pet = "Max",
        maxDate = "Mar 14, 2025",
        daysOverdue = 349)
}