package com.example.petcare.ui.screens.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petcare.R
import com.example.petcare.ui.components.Filters
import com.example.petcare.ui.components.PetDetailsCard
import com.example.petcare.ui.components.SearchBar
import com.example.petcare.ui.theme.GreenAccentDark
import com.example.petcare.ui.theme.OffWhite

data class PetListItem(
    val name: String,
    val breed: String,
    val age: Int,
    val weight: Double,
    val gender: String,
    val status: String,
    val photoPath: Int,
    val species: String
)
@Composable
fun PetsScreen(
    onPetSelected: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onVaccineSelected: (String) -> Unit = {},
    onLostModeSelected: (String) -> Unit = {},
    onNfcSelected: () -> Unit = {}

) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All Pets") }

    val pets = listOf(
        PetListItem("Max", "Golden Retriever", 6, 28.5, "Male", "Healthy", R.drawable.pet, "DOG"),
        PetListItem("Luna", "Tabby Mix", 5, 4.2, "Female", "Vaccine due", R.drawable.pet, "CAT"),
        PetListItem("Coco", "Poodle", 4, 1.6, "Female", "Healthy", R.drawable.pet, "DOG")
    )

    
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

        // Search
        item {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
        }

        // Filters
        item {
            Filters(
                filters = listOf("All Pets", "Healthy", "Vaccine Due", "Lost"),
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )
        }

        items(pets) { pet ->
            PetDetailsCard(
                petName = pet.name,
                breed = pet.breed,
                age = pet.age,
                weight = pet.weight,
                gender = pet.gender,
                status = pet.status,
                photoPath = pet.photoPath,
                species = pet.species,
                onPetSelect = { onPetSelected(pet.name) },
                onVaccineSelect = { onVaccineSelected(pet.name) },
                onLostSelect = { onLostModeSelected(pet.name) },
                onNFCSelect = { onNfcSelected() }
            )
        }
    }
}




@Preview(showBackground = true)
@Composable
fun PetScreenPreview(){
    PetsScreen(
        onPetSelected = {}
    )
}
