package com.example.petcare.ui.screens.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.data.model.GroupedSuggestion
import com.example.petcare.ui.theme.ErrorContainer
import com.example.petcare.ui.theme.ErrorContent

@Composable
fun NotificationsScreen(
    filterPetId: String? = null,
    filterPetName: String? = null,
    onBack: () -> Unit = {},
    viewModel: NotificationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(filterPetId) {
        viewModel.load(filterPetId, filterPetName)
    }

    val title = if (filterPetName != null) "$filterPetName's alerts" else "All alerts"

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            else -> {
                LazyColumn(
                    modifier              = Modifier.fillMaxSize().padding(padding),
                    contentPadding        = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp)
                ) {

                    if (uiState.availablePets.size > 1) {
                        item {
                            PetFilterRow(
                                chips      = uiState.availablePets,
                                selectedId = uiState.selectedPetId,
                                onSelect   = viewModel::onPetFilterSelected
                            )
                        }
                    }

                    if (uiState.displayed.isEmpty()) {
                        item {
                            Box(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 80.dp),
                                contentAlignment  = Alignment.Center
                            ) {
                                Text(
                                    text  = "No alerts for this pet",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(
                            items = uiState.displayed,
                            key   = { it.vaccineTitle }
                        ) { grouped ->
                            GroupedSuggestionCard(
                                grouped  = grouped
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PetFilterRow(
    chips: List<PetFilterChip>,
    selectedId: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chip "All"
        item {
            FilterChip(
                selected = selectedId == null,
                onClick  = { onSelect(null) },
                label    = {
                    Text(
                        text     = "All",
                        fontSize = 13.sp
                    )
                }
            )
        }
        items(chips) { chip ->
            FilterChip(
                selected = selectedId == chip.petId,
                onClick  = { onSelect(chip.petId) },
                label    = {
                    Text(
                        text     = "${chip.petName} (${chip.alertCount})",
                        fontSize = 13.sp
                    )
                }
            )
        }
    }
}

@Composable
fun GroupedSuggestionCard(grouped: GroupedSuggestion) {
    val (background, iconColor, icon) = grouped.uiConfig()
    val petsLabel = when (grouped.pets.size) {
        1    -> grouped.pets.first()
        2    -> "${grouped.pets[0]} and ${grouped.pets[1]}"
        else -> "${grouped.pets.take(2).joinToString(", ")} +${grouped.pets.size - 2} more"
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier                = Modifier.padding(14.dp),
            horizontalArrangement   = Arrangement.spacedBy(12.dp),
            verticalAlignment       = Alignment.Top
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconColor,
                modifier           = Modifier.size(22.dp).padding(top = 2.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = grouped.vaccineTitle,
                    fontWeight = FontWeight.Bold,
                    color      = iconColor,
                    fontSize   = 14.sp
                )
                Text(
                    text     = grouped.message,
                    color    = iconColor,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = petsLabel,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color      = iconColor.copy(alpha = 0.75f)
                )
            }
        }
    }
}

private data class CardConfig(
    val background: Color,
    val iconColor: Color,
    val icon: ImageVector
)

@Composable
private fun GroupedSuggestion.uiConfig() = when (type) {
    "danger"  -> CardConfig(ErrorContainer, ErrorContent, Icons.Default.Warning)
    "warning" -> CardConfig(Color(0xFFFFF3CD), Color(0xFFB45309), Icons.Default.Info)
    else      -> CardConfig(Color(0xFFE8F4FD), Color(0xFF1565C0), Icons.Default.Notifications)
}