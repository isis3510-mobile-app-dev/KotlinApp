package com.example.petcare.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.R
import com.example.petcare.ui.components.*
import com.example.petcare.ui.screens.auth.AuthViewModel
import com.example.petcare.ui.theme.GreenDark

@Composable
fun HomeScreen(
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onNavigateToPets: () -> Unit,
    onNavigateToNfc: () -> Unit,
    onNavigateToPetProfile: (String) -> Unit,
    onNavigateToAddPet: () -> Unit,
    onNavigateToVaccine: (String, String) -> Unit = { _, _ -> },
    onNavigateToRecords: () -> Unit = {},
    onNavigateToEvent: (String, String) -> Unit = { _, _ -> },
    // Passed in from MainActivity so both screens share the same instance
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel = viewModel()
) {
    val homeState   by homeViewModel.state.collectAsStateWithLifecycle()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()

    // As soon as the logged-in user's profile is available, push name+id into HomeViewModel
    LaunchedEffect(userProfile) {
        userProfile?.let {
            homeViewModel.setUserInfo(
                name   = it.name.split(" ").firstOrNull() ?: it.name,
                userId = it.id
            )
        }
    }

    // Reload pets & events every time this screen is visited
    LaunchedEffect(Unit) { homeViewModel.loadData() }

    val displayName = userProfile?.name?.split(" ")?.firstOrNull()
        ?: homeState.userName.ifBlank { "Hello" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayName,
                    fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NfcButton(onClick = onNavigateToNfc)
                    NotificationButton()
                }
            }
        }

        // ── Loading / Error state ─────────────────────────────────────────────
        if (homeState.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = GreenDark) }
            }
            return@LazyColumn
        }

        homeState.error?.let { err ->
            item {
                Text(
                    text = "⚠ $err", color = Color.Red,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // ── My Pets ───────────────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "My Pets", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigateToPets() }
                    ) {
                        Text(
                            "See all", fontSize = 16.sp,
                            fontWeight = FontWeight.Medium, color = GreenDark
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null, tint = GreenDark
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    // Real pets from backend
                    items(homeState.pets.size) { i ->
                        val pet = homeState.pets[i]
                        Box(modifier = Modifier.clickable { onNavigateToPetProfile(pet.id) }) {
                            PetCard(
                                image  = painterResource(R.drawable.pet),
                                text   = pet.name,
                                status = when (pet.status.lowercase()) {
                                    "healthy" -> PetStatus.SUCCESS
                                    else      -> PetStatus.WARNING
                                }
                            )
                        }
                    }
                    // Add-pet button
                    item {
                        Box(modifier = Modifier.clickable { onNavigateToAddPet() }) {
                            PetCard()
                        }
                    }
                }
            }
        }

        // ── Active Events ─────────────────────────────────────────────────────
        if (homeState.recentEvents.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Active Events", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "View all", fontSize = 16.sp,
                            fontWeight = FontWeight.Medium, color = GreenDark,
                            modifier = Modifier.clickable { onNavigateToRecords() }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        homeState.recentEvents.take(3).forEach { event ->
                            Box(modifier = Modifier.clickable {
                                onNavigateToEvent(event.petId, event.id)
                            }) {
                                EventCard(
                                    eventName = event.title,
                                    pet = homeState.pets.find { it.id == event.petId }?.name ?: "",
                                    date = event.date.take(10)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}