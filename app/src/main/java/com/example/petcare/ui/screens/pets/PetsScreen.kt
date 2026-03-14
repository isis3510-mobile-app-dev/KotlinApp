package com.example.petcare.ui.screens.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
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
import com.example.petcare.ui.components.NavBar
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
    currentRoute: String,
    onNavigateTab: (String) -> Unit,
    onPetSelected: (String) -> Unit
    ){

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All Pets") }


    val pets = listOf(
        PetListItem("Max", "Golden Retriever", 6, 28.5, "Male", "Healthy", R.drawable.pet, "DOG"),
        PetListItem("Luna", "Tabby Mix", 5, 4.2, "Female", "Vaccine due", R.drawable.pet, "CAT"),
        PetListItem("Coco", "Poodle", 4, 1.6, "Female", "Healthy", R.drawable.pet, "DOG")
    )

    Scaffold(
        bottomBar = {
            NavBar(
                currentRoute = currentRoute,
                onItemClick = onNavigateTab
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


            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            Filters(
                filters = listOf("All Pets", "Healthy", "Vaccine Due", "Lost"),
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(pets) { pet ->
                    Box(modifier = Modifier.clickable { onPetSelected(pet.name) }) {
                        PetDetailsCard(
                            petName = pet.name,
                            breed = pet.breed,
                            age = pet.age,
                            weight = pet.weight,
                            gender = pet.gender,
                            status = pet.status,
                            photoPath = pet.photoPath,
                            species = pet.species
                        )
                    }
                }
            }
          }
        }
    }



@Preview(showBackground = true)
@Composable
fun PetScreenPreview(){
    PetsScreen(
        currentRoute = "pets",
        onNavigateTab = {},
        onPetSelected = {}
    )
}