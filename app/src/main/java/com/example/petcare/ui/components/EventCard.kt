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
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.GreenLight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.theme.Black
import com.example.petcare.util.DisplayTextLimits
import com.example.petcare.util.truncateForDisplay

@Composable
fun EventCard(eventName: String,pet:String, date: String){
    Card(modifier = Modifier
        .fillMaxWidth()
        //.padding(16.dp)
        .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ){
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = "Event",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = eventName.truncateForDisplay(DisplayTextLimits.COMPACT_TITLE),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text (
                        text = "${pet.truncateForDisplay(DisplayTextLimits.SUBTITLE_META)} - ",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text (
                        text = date.truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )}
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(GreenLight).padding(horizontal = 10.dp, vertical = 4.dp)

            ){
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Event",
                        tint = Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                    text = "Reminder on",
                    fontSize = 12.sp,
                    color = GreenDark,
                    fontWeight = FontWeight.Medium)
                }

            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun EventCardPreview () {
    EventCard(eventName ="Vet Visit",
        pet = "Max",
        date = "March 3, 2025")
}
