package com.example.petcare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.R
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.SuccessContent
import com.example.petcare.ui.theme.WarningYellow

data class ActiveVaccineListItemData(
    val vaccineName: String,
    val petName: String,
    val dateVaccine: String,
    val doctor: String,
    val photoPath: Int
)

@Composable
fun ActiveVaccineCard(vaccine: ActiveVaccineListItemData){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Image(
                painter = painterResource(id = vaccine.photoPath),
                contentDescription = vaccine.petName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vaccine.vaccineName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${vaccine.petName} · ${vaccine.dateVaccine}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Dr. ${vaccine.doctor}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ){
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(SuccessContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Active", fontSize = 11.sp, color = SuccessContent)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ){
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notification",
                        tint = WarningYellow,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reminder", fontSize = 11.sp, color = WarningYellow)
                }
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun ActiveVaccineCardPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        ActiveVaccineCard(ActiveVaccineListItemData("Rabies", "Max", "24-02-2026", "Dr. Smith", R.drawable.pet))

    }
}
