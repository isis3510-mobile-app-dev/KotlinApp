package com.example.petcare.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.R
import com.example.petcare.ui.components.ActiveVaccineCard
import com.example.petcare.ui.components.ActiveVaccineListItemData
import com.example.petcare.ui.components.EventCard
import com.example.petcare.ui.components.ExpandableFAB
import com.example.petcare.ui.components.NavBar
import com.example.petcare.ui.components.NfcButton
import com.example.petcare.ui.components.NotificationButton
import com.example.petcare.ui.components.OverdueWarningBanner
import com.example.petcare.ui.components.PetCard
import com.example.petcare.ui.components.PetStatus
import com.example.petcare.ui.theme.GreenDark
import com.example.petcare.ui.theme.OffWhite

@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableStateOf("home") }

    Scaffold(
        floatingActionButton = { ExpandableFAB() },
        bottomBar = {
            NavBar(
                currentRoute = selectedTab,
                onItemClick = { selectedTab = it }
            )
        },
        containerColor = OffWhite
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sarah",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NfcButton()
                        NotificationButton()
                    }
                }
            }

            // My Pets Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Pets",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {  }) {
                            Text(
                                text = "See all",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = GreenDark
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "See all pets",
                                tint = GreenDark
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        item {
                            PetCard(
                                image = painterResource(R.drawable.pet),
                                text = "Max",
                                status = PetStatus.SUCCESS
                            )
                        }
                        item {
                            PetCard(
                                image = painterResource(R.drawable.pet),
                                text = "Luna",
                                status = PetStatus.WARNING
                            )
                        }
                        item {
                            PetCard(
                                image = painterResource(R.drawable.pet),
                                text = "Coco",
                                status = PetStatus.SUCCESS
                            )
                        }
                        item {
                            PetCard()
                        }
                    }
                }
            }

            // Vaccines Banner & Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OverdueWarningBanner(overdueCount = 2)
                    
                    // Note: Reusing ActiveVaccineCard for now as requested.
                    // This card has slightly different fields than the "NEXT VACCINE" component in the screenshot
                    ActiveVaccineCard(
                        vaccine = ActiveVaccineListItemData(
                            vaccineName = "Rabies",
                            petName = "Max",
                            dateVaccine = "Mar 14, 2025",
                            doctor = "Smith", // ActiveVaccineCard requires a doctor
                            photoPath = R.drawable.pet
                        )
                    )
                }
            }

            // Active Events Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Events",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {  }) {
                            Text(
                                text = "View all",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = GreenDark
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy((-16).dp)) { 
                        // EventCard has an internal 16dp padding
                        EventCard(eventName = "Vet Check-up", pet = "Max", date = "March 3")
                        EventCard(eventName = "Vet Check-up", pet = "Max", date = "October 14")
                        EventCard(eventName = "Vaccination Day", pet = "Luna", date = "May 2")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
