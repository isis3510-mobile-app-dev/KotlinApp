package com.example.petcare.ui.screens.records

import com.example.petcare.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.R
import com.example.petcare.ui.components.ActiveVaccineCard
import com.example.petcare.ui.components.ActiveVaccineListItemData
import com.example.petcare.ui.components.ExpandableFAB
import com.example.petcare.ui.components.Filters
import com.example.petcare.ui.components.MedicalEventData
import com.example.petcare.ui.components.MedicalEventItem
import com.example.petcare.ui.components.NavBar
import com.example.petcare.ui.components.OverdueWarningBanner
import com.example.petcare.ui.components.VaccineListItem
import com.example.petcare.ui.components.VaccineListItemData

@Composable
private fun StatusChip(label: String, background: Color, textColor: Color){
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ){
        Text(
            label,
            fontSize = 11.sp,
            color = textColor
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    badges: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.weight(1f))
        badges()
    }
}

@Composable
fun HealthRecordsScreen(){
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedTab by remember { mutableStateOf("records") }

    Scaffold(
        floatingActionButton = { ExpandableFAB() },
        bottomBar = {
            NavBar(
                currentRoute = selectedTab,
                onItemClick = {}
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Health Records",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Filters(
                filters = listOf("All", "Vaccines", "Events"),
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            when (selectedFilter){
                "All" -> AllRecordsContent()
                "Vaccines" -> VaccinesContent()
                "Events" -> EventsContent()
            }
        }

    }
}

@Composable
private fun AllRecordsContent(){
    val vaccines = listOf(
        VaccineListItemData("Leptospirosis", "Max", "City Vet Center", "overdue", "493d ago", R.drawable.pet),
        VaccineListItemData("FVRCP (Core)", "Luna", "Cat Care Center", "overdue", "377d ago", R.drawable.pet),
        VaccineListItemData("Rabies", "Max", "Happy Paws Clinic", "upcoming", "in -349d", R.drawable.pet),
        VaccineListItemData("FeLV", "Luna", "Cat Care Center", "upcoming", "in 33d", R.drawable.pet),
        VaccineListItemData("DHPP (Core)", "Max", "Happy Paws Clinic", "completed", "done", R.drawable.pet),
        VaccineListItemData("Bordetella", "Max", "Happy Paws Clinic", "completed", "done", R.drawable.pet),
    )

    OverdueWarningBanner(overdueCount = 2)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Vaccines",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        StatusChip("2 overdue", ErrorContainer, ErrorContent)
        Spacer(modifier = Modifier.width(8.dp))
        StatusChip("2 upcoming", InfoContainer, InfoContent)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(vaccines){ vaccine ->
            VaccineListItem(vaccine=vaccine)
            HorizontalDivider(color = GrayBackground)
        }
    }
}

@Composable
private fun VaccinesContent() {
    val vaccines = listOf(
        ActiveVaccineListItemData("Rabbies", "Max", "24-02-2026", "Dr. Smith", R.drawable.pet),
        ActiveVaccineListItemData("Rabies", "Max", "Happy Paws Clinic", "upcoming",  R.drawable.pet),
        ActiveVaccineListItemData(" (Core)", "Max", "Happy Paws Clinic", "completed",  R.drawable.pet)
    )

    SectionHeader(
        title = "Vaccines",
        badges = {
            StatusChip("3 active", SuccessContainer, SuccessContent)
        }
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(vaccines) { vaccine ->
            ActiveVaccineCard(vaccine = vaccine)
        }
    }
}

@Composable
private fun EventsContent() {
    val events = listOf(
        MedicalEventData("Checkup", "Max", "Happy Paws Clinic", "Nov 19, 2024", "$120"),
        MedicalEventData("Checkup", "Luna", "Cat Care Center", "Oct 14, 2024", "$95"),
        MedicalEventData("Emergency", "Luna", "City Animal Emergency", "Aug 29, 2024", "$340"),
        MedicalEventData("Dental", "Max", "City Vet Center", "Jun 4, 2024", "$280"),
    )

    SectionHeader(title = "Medical Events", badges = {})

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(events) { event ->
            MedicalEventItem(event = event)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AllRecordsPreview() {
    HealthRecordsScreen()
}