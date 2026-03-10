package com.example.petcare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.example.petcare.R
import com.example.petcare.ui.theme.ErrorContainer
import com.example.petcare.ui.theme.ErrorContent
import com.example.petcare.ui.theme.GrayMedium
import com.example.petcare.ui.theme.GrayText
import com.example.petcare.ui.theme.InfoContainer
import com.example.petcare.ui.theme.InfoContent
import com.example.petcare.ui.theme.SuccessContainer
import com.example.petcare.ui.theme.SuccessContent

data class VaccineListItemData(
    val vaccineName: String,
    val petName: String,
    val clinicName: String,
    val status: String,       // "overdue", "upcoming", "completed"
    val daysText: String,     // "493d ago", "in -349d", etc
    val photoPath: Int
)

@Composable
fun VaccineListItem(vaccine: VaccineListItemData){
    val (badgeColor, textColor) = when (vaccine.status.lowercase()){
        "overdue" -> Pair(ErrorContainer, ErrorContent)
        "upcoming" -> Pair(InfoContainer, InfoContent)
        "completed" -> Pair(SuccessContainer, SuccessContent)
        else -> Pair(GrayMedium, GrayText)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.pet),
            contentDescription = vaccine.petName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width((12.dp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = vaccine.vaccineName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = "${vaccine.petName} · ${vaccine.clinicName}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(badgeColor)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = vaccine.status,
                    fontSize = 11.sp,
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = vaccine.daysText,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VaccineListItemPreview() {
    Column {
        VaccineListItem(VaccineListItemData("Leptospirosis", "Max", "City Vet Center", "overdue", "493d ago", R.drawable.pet))
        VaccineListItem(VaccineListItemData("Rabies", "Max", "Happy Paws Clinic", "upcoming", "in -349d", R.drawable.pet))
        VaccineListItem(VaccineListItemData(" (Core)", "Max", "Happy Paws Clinic", "completed", "done", R.drawable.pet))
    }
}