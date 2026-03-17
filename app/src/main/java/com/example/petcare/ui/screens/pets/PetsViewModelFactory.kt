package com.example.petcare.ui.screens.pets


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.petcare.data.repository.PetRepository

class PetsViewModelFactory(
    private val petRepository: PetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PetsViewModel::class.java)) {
            return PetsViewModel(petRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}