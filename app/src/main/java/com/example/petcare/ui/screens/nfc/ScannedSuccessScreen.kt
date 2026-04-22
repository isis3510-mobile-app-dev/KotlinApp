package com.example.petcare.ui.screens.nfc

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petcare.MainActivity
import com.example.petcare.data.repository.NfcPetPayload
import com.example.petcare.ui.theme.*
import com.example.petcare.util.DisplayTextLimits
import com.example.petcare.util.truncateForDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedSuccessScreen(
    onBack: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    val activity     = LocalActivity.current as MainActivity
    val nfcViewModel = activity.nfcViewModel
    val uiState by nfcViewModel.uiState.collectAsStateWithLifecycle()

    val payload = (uiState as? NfcUiState.ReadSuccess)?.payload

    ScannedSuccessContent(
        payload = payload,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedSuccessContent(
    payload: NfcPetPayload?,
    onBack: () -> Unit
) {
    val isLost = payload?.status?.equals("lost", ignoreCase = true) == true

    val hasAllergies = !payload?.knownAllergies.isNullOrBlank()
    val hasVetInfo   = !payload?.defaultVet.isNullOrBlank() || !payload?.defaultClinic.isNullOrBlank()
    val hasMedNotes  = hasAllergies || hasVetInfo

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC Tag", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ── Success indicator ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SuccessContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Check,
                    contentDescription = "Success",
                    tint               = SuccessGreen,
                    modifier           = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text       = "Tag scanned successfully",
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Main card ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column {

                    // ── Card header: pet identity + status ────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GreenDark)
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier.weight(1f)
                            ) {
                                // Paw / warning icon circle
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLost) {
                                        Icon(
                                            imageVector        = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint               = Color.White,
                                            modifier           = Modifier.size(26.dp)
                                        )
                                    } else {
                                        Text("🐾", fontSize = 22.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text       = (payload?.petName ?: "Unknown pet")
                                            .truncateForDisplay(DisplayTextLimits.COMPACT_TITLE),
                                        fontSize   = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subtitle = buildString {
                                        if (!payload?.breed.isNullOrBlank())   append(payload!!.breed)
                                        if (!payload?.breed.isNullOrBlank() &&
                                            !payload?.species.isNullOrBlank()) append(" - ")
                                        if (!payload?.species.isNullOrBlank()) append(payload!!.species)
                                    }
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text     = subtitle
                                                .truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                                            fontSize = 13.sp,
                                            color    = Color.White.copy(alpha = 0.82f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Status pill
                            val pillTextColor = if (isLost) ErrorContent else GreenDark
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector        = if (isLost) Icons.Default.Warning else Icons.Default.Check,
                                    contentDescription = null,
                                    tint               = pillTextColor,
                                    modifier           = Modifier.size(12.dp)
                                )
                                Text(
                                    text       = if (isLost) "Lost" else "Healthy",
                                    fontSize   = 12.sp,
                                    color      = pillTextColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ── Card body ─────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {

                        // Owner section
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text       = "Owner Information",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val initials = payload?.ownerInitials?.ifBlank {
                                    payload.ownerName
                                        .split(" ")
                                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                        .take(2)
                                        .joinToString("")
                                } ?: "?"

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(GreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text       = initials,
                                        fontSize   = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = GreenDark
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text       = (payload?.ownerName ?: "—")
                                            .truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text     = (payload?.ownerPhone ?: "—")
                                            .truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                                        fontSize = 14.sp,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Divider
                        if (hasMedNotes) {
                            HorizontalDivider(color = GrayMedium)
                        }

                        // Medical notes section
                        if (hasMedNotes) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint     = WarningContent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text       = "Medical Notes",
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = WarningContent,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                // Allergies — most critical, highlighted
                                if (hasAllergies) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(WarningContainer)
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text       = "Known Allergies",
                                                fontSize   = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color      = WarningContent,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text     = payload!!.knownAllergies
                                                    .truncateForDisplay(DisplayTextLimits.LONG_SNIPPET),
                                                fontSize = 13.sp,
                                                color    = WarningContent,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Vet / Clinic row
                                if (hasVetInfo) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (!payload?.defaultVet.isNullOrBlank()) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text       = "VET",
                                                    fontSize   = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color      = GrayText,
                                                    letterSpacing = 0.8.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text     = payload!!.defaultVet
                                                        .truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                                                    fontSize = 13.sp,
                                                    color    = MaterialTheme.colorScheme.onBackground,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (!payload?.defaultClinic.isNullOrBlank()) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text       = "CLINIC",
                                                    fontSize   = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color      = GrayText,
                                                    letterSpacing = 0.8.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text     = payload!!.defaultClinic
                                                        .truncateForDisplay(DisplayTextLimits.SUBTITLE_META),
                                                    fontSize = 13.sp,
                                                    color    = MaterialTheme.colorScheme.onBackground,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Healthy + allergies")
@Composable
fun ScannedSuccessPreviewHealthy() {
    PetCareTheme {
        ScannedSuccessContent(
            payload = NfcPetPayload(
                petId          = "1",
                petName        = "yacko",
                species        = "dog",
                breed          = "Maltes",
                ownerName      = "Lina Muñoz",
                ownerPhone     = "3142563652",
                ownerInitials  = "LM",
                status         = "healthy",
                knownAllergies = "Pollen, chicken protein",
                defaultVet     = "Dr. Maf",
                defaultClinic  = "Colmillos"
            ),
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Lost pet")
@Composable
fun ScannedSuccessPreviewLost() {
    PetCareTheme {
        ScannedSuccessContent(
            payload = NfcPetPayload(
                petId          = "2",
                petName        = "Luna",
                species        = "cat",
                breed          = "Siamese",
                ownerName      = "Carlos Pérez",
                ownerPhone     = "3001234567",
                ownerInitials  = "CP",
                status         = "lost",
                knownAllergies = "",
                defaultVet     = "Dr. García",
                defaultClinic  = "VetPlus"
            ),
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "No medical notes")
@Composable
fun ScannedSuccessPreviewNoNotes() {
    PetCareTheme {
        ScannedSuccessContent(
            payload = NfcPetPayload(
                petId         = "3",
                petName       = "Rex",
                species       = "dog",
                breed         = "Labrador",
                ownerName     = "Ana Torres",
                ownerPhone    = "3109876543",
                ownerInitials = "AT",
                status        = "healthy"
            ),
            onBack = {}
        )
    }
}
