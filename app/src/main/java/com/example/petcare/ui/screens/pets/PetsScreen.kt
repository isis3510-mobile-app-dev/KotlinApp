package com.example.petcare.ui.screens.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.data.model.Pet
import com.example.petcare.ui.components.Filters
import com.example.petcare.ui.components.PetDetailsCard
import com.example.petcare.ui.components.SearchBar
import com.example.petcare.ui.theme.GreenAccentDark
import com.example.petcare.ui.theme.OffWhite
import java.time.LocalDate
import java.time.Period

private fun calculateAge(birthDate: String?): Int {
    if (birthDate == null) return 0
    return try {
        val birth = java.time.LocalDate.parse(birthDate.take(10))
        java.time.Period.between(birth, java.time.LocalDate.now()).years
    } catch (e: Exception) { 0 }
}

@Composable
fun PetsScreen(
    pets: List<Pet>,
    isLoading: Boolean = false,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    selectedFilter: String = "All Pets",
    onFilterSelected: (String) -> Unit = {},
    onPetSelected: (String) -> Unit,
    onVaccineSelected: (String) -> Unit = {},
    onLostModeSelected: (String) -> Unit = {},
    onNfcSelected: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = paddingValues.calculateTopPadding() + 20.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row {
                Text(
                    text = "My Pets",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(OffWhite)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${pets.size} pets",
                        color = GreenAccentDark,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Search Bar
        item {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange
            )
        }

        // Filters Row
        item {
            Filters(
                filters = listOf("All Pets", "Healthy", "Vaccine Due", "Lost"),
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected
            )
        }

        // 2. Display filtered results
        items(pets, key = { it.id }) { pet ->
            PetDetailsCard(
                petName = pet.name,
                breed = pet.breed,
                age = calculateAge(pet.birthDate),
                weight = pet.weight ?: 0.0,
                gender = pet.gender,
                status = pet.status,
                photoPath = pet.photoUrl,
                species = pet.species,
                onPetSelect = { onPetSelected(pet.id) },
                onVaccineSelect = { onVaccineSelected(pet.id) },
                onLostSelect = { onLostModeSelected(pet.id) },
                onNFCSelect = { onNfcSelected(pet.id) }
            )
        }

        // Empty State
        if (pets.isEmpty() && !isLoading) {
            item {
                Box(Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp)) {
                    Text("No pets found matching your search.", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}