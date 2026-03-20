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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.ui.components.*
import com.example.petcare.data.analytics.FeatureClicksTracker
import com.example.petcare.ui.screens.auth.AuthViewModel
import com.example.petcare.ui.theme.*
import com.example.petcare.util.DisplayTextLimits
import com.example.petcare.util.EventDateUtils
import com.example.petcare.util.truncateForDisplay

@Composable
fun HomeScreen(
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onNavigateToPets: () -> Unit,
    onNavigateToNfc: () -> Unit,
    onNavigateToPetProfile: (String) -> Unit,
    onNavigateToAddPet: () -> Unit,
    // petId + vaccinationId (embedded _id)
    onNavigateToVaccine: (String, String) -> Unit = { _, _ -> },
    onNavigateToRecords: () -> Unit = {},
    onNavigateToEvent: (String, String) -> Unit = { _, _ -> },
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel = viewModel(),
    onNavigateToSuggestions: () -> Unit
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
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = displayName.truncateForDisplay(DisplayTextLimits.COMPACT_TITLE),
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NfcButton(onClick = onNavigateToNfc)
                    NotificationButton()
                }
            }
        }

        // ── Loading / Error ───────────────────────────────────────────────────
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
                Text("⚠ $err", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }

        // ── My Pets ───────────────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "My Pets", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.clickable {
                            FeatureClicksTracker.recordClick()
                            onNavigateToPets()
                        }
                    ) {
                        Text("See all", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = GreenDark)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding        = PaddingValues(horizontal = 4.dp)
                ) {
                    items(homeState.pets.size) { i ->
                        val pet = homeState.pets[i]
                        Box(modifier = Modifier.clickable {
                            FeatureClicksTracker.recordClick()
                            onNavigateToPetProfile(pet.id)
                        }) {
                            PetCard(
                                photoUrl = pet.photoUrl,
                                text     = pet.name,
                                status   = when (pet.status.lowercase()) {
                                    "healthy" -> PetStatus.SUCCESS
                                    else      -> PetStatus.WARNING
                                }
                            )
                        }
                    }
                    item {
                        Box(modifier = Modifier.clickable {
                            FeatureClicksTracker.recordClick()
                            FeatureClicksTracker.startRoute("Add Pet Flow")
                            onNavigateToAddPet()
                        }) {
                            PetCard()
                        }
                    }
                }
            }
        }

        // ── Health Alerts ─────────────────────────────────────────────────────
        if (homeState.topAlert != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "Health Alerts",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        if (homeState.totalAlertCount >= 1) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier.clickable {
                                    FeatureClicksTracker.recordClick()
                                    onNavigateToSuggestions()
                                }
                            ) {
                                Text(
                                    text       = "See all ${homeState.totalAlertCount}",
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color      = MaterialTheme.colorScheme.secondary
                                )
                                Icon(
                                    imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    GroupedSuggestionCard(grouped = homeState.topAlert!!)
                }
            }
        }

        // ── Upcoming Vaccines ─────────────────────────────────────────────────
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
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "See all",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color      = GreenDark,
                            modifier   = Modifier.clickable {
                                FeatureClicksTracker.recordClick()
                                onNavigateToRecords()
                            }
                        )
                    }

                    homeState.upcomingVaccines.take(3).forEach { vacc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                // ← Ahora navega a VaccineDetails con vaccinationId real
                                .clickable {
                                    FeatureClicksTracker.recordClick()
                                    if (vacc.vaccinationId.isNotBlank()) {
                                        onNavigateToVaccine(vacc.petId, vacc.vaccinationId)
                                    } else {
                                        onNavigateToPetProfile(vacc.petId)
                                    }
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
                                        imageVector        = Icons.Default.Vaccines,
                                        contentDescription = null,
                                        tint               = if (vacc.daysUntilDue <= 7) ErrorContent else InfoContent,
                                        modifier           = Modifier.size(24.dp)
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
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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

        // ── Recent Events ─────────────────────────────────────────────────────
        if (homeState.recentEvents.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "Active Events", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "View all",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.secondary,
                            modifier   = Modifier.clickable { onNavigateToRecords() }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        homeState.recentEvents.take(3).forEach { event ->
                            Box(modifier = Modifier.clickable {
                                FeatureClicksTracker.recordClick()
                                onNavigateToEvent(event.petId, event.id)
                            }) {
                                EventCard(
                                    eventName = event.title,
                                    pet       = homeState.pets.find { it.id == event.petId }?.name ?: "",
                                    date      = EventDateUtils.splitToAppDateTime(event.date).first
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
