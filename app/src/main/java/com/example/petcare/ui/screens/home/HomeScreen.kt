package com.example.petcare.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.ui.components.*
import com.example.petcare.ui.screens.auth.AuthViewModel
import com.example.petcare.ui.theme.*

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
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel = viewModel()
) {
    val homeState   by homeViewModel.state.collectAsStateWithLifecycle()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()

    LaunchedEffect(userProfile) {
        userProfile?.let {
            homeViewModel.setUserInfo(
                name   = it.name.split(" ").firstOrNull() ?: it.name,
                userId = it.id
            )
        }
    }

    LaunchedEffect(Unit) { homeViewModel.loadData() }

    val displayName = userProfile?.name?.split(" ")?.firstOrNull()
        ?: homeState.userName.ifBlank { "Hello" }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        item {
            Row(
                modifier            = Modifier.fillMaxWidth(),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = displayName,
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.Black
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NfcButton(onClick = onNavigateToNfc)
                    NotificationButton()
                }
            }
        }

        // ── Loading / Error ───────────────────────────────────────────────
        if (homeState.isLoading) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = GreenDark) }
            }
            return@LazyColumn
        }

        homeState.error?.let { err ->
            item {
                Text("⚠ $err", color = Color.Red, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }

        // ── My Pets ───────────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("My Pets", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.clickable { onNavigateToPets() }
                    ) {
                        Text("See all", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = GreenDark)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = GreenDark)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding        = PaddingValues(horizontal = 4.dp)
                ) {
                    items(homeState.pets.size) { i ->
                        val pet = homeState.pets[i]
                        Box(modifier = Modifier.clickable { onNavigateToPetProfile(pet.id) }) {
                            PetCard(
                                photoUrl  = pet.photoUrl,
                                text   = pet.name,
                                status = when (pet.status.lowercase()) {
                                    "healthy" -> PetStatus.SUCCESS
                                    else      -> PetStatus.WARNING
                                }
                            )
                        }
                    }
                    item {
                        Box(modifier = Modifier.clickable { onNavigateToAddPet() }) {
                            PetCard()
                        }
                    }
                }
            }
        }

        // ── Overdue warning ───────────────────────────────────────────────
        if (homeState.overdueVaccinesCount > 0) {
            item {
                OverdueWarningBanner(overdueCount = homeState.overdueVaccinesCount)
            }
        }

        // ── Upcoming Vaccines (next 30 days) ──────────────────────────────
        if (homeState.upcomingVaccines.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "Upcoming Vaccines",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.Black
                        )
                        Text(
                            "See all",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color      = GreenDark,
                            modifier   = Modifier.clickable { onNavigateToRecords() }
                        )
                    }

                    homeState.upcomingVaccines.take(3).forEach { vacc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .clickable {
                                    // Navigate to pet profile vaccines tab
                                    onNavigateToPetProfile(vacc.petId)
                                }
                                .padding(16.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier         = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (vacc.daysUntilDue <= 7) ErrorContainer else InfoContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector     = Icons.Default.Vaccines,
                                        contentDescription = null,
                                        tint            = if (vacc.daysUntilDue <= 7) ErrorContent else InfoContent,
                                        modifier        = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text       = vacc.vaccineName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize   = 14.sp
                                    )
                                    Text(
                                        text     = "${vacc.petName} · Due ${vacc.dueDate}",
                                        fontSize = 12.sp,
                                        color    = Color.Gray
                                    )
                                }
                            }
                            // Days badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(if (vacc.daysUntilDue <= 7) ErrorContainer else InfoContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text       = if (vacc.daysUntilDue == 0L) "Today"
                                    else "in ${vacc.daysUntilDue}d",
                                    fontSize   = 12.sp,
                                    color      = if (vacc.daysUntilDue <= 7) ErrorContent else InfoContent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Recent Events ─────────────────────────────────────────────────
        if (homeState.recentEvents.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Events",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.Black
                        )
                        Text(
                            "View all",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color      = GreenDark,
                            modifier   = Modifier.clickable { onNavigateToRecords() }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        homeState.recentEvents.take(3).forEach { event ->
                            Box(modifier = Modifier.clickable {
                                onNavigateToEvent(event.petId, event.id)
                            }) {
                                EventCard(
                                    eventName = event.title,
                                    pet       = homeState.pets.find { it.id == event.petId }?.name ?: "",
                                    date      = event.date.take(10)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}