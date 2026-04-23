package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.petcare.data.local.entity.Pet
import com.example.petcare.data.local.entity.PetWithVaccines
import com.example.petcare.data.local.entity.Vaccine
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {

    @Transaction
    @Query("SELECT * FROM pets")
    fun getPetsWithVaccines(): Flow<List<PetWithVaccines>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: Pet): String

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccine(vaccine: Vaccine)

    @Query("SELECT * FROM vaccinations WHERE petId = :petId")
    fun getVaccinesByPet(petId: String): Flow<List<Vaccine>>

    @Delete
    suspend fun deletePet(pet: Pet)
}