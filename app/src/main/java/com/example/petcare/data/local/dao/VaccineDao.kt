package com.example.petcare.data.local.dao

import androidx.room.*
import com.example.petcare.data.local.entity.VaccinationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccineDao {

    @Query("SELECT * FROM vaccinations WHERE petId = :petId AND pendingDelete = 0")
    fun getVaccinesForPet(petId: String): Flow<List<VaccinationEntity>>

    @Query("SELECT * FROM vaccinations WHERE petId = :petId AND pendingDelete = 0")
    suspend fun getVaccinesForPetSync(petId: String): List<VaccinationEntity>

    @Query("SELECT * FROM vaccinations WHERE pendingSync = 1 AND pendingDelete = 0")
    suspend fun getPendingSync(): List<VaccinationEntity>

    @Query("SELECT * FROM vaccinations WHERE pendingDelete = 1")
    suspend fun getPendingDelete(): List<VaccinationEntity>

    @Query("SELECT * FROM vaccinations WHERE id = :id")
    suspend fun getById(id: String): VaccinationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccine(vaccine: VaccinationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vaccines: List<VaccinationEntity>)

    @Update
    suspend fun updateVaccine(vaccine: VaccinationEntity)

    @Query("DELETE FROM vaccinations WHERE id = :id")
    suspend fun deleteVaccineById(id: String)

    @Query("DELETE FROM vaccinations WHERE petId = :petId")
    suspend fun deleteAllForPet(petId: String)
}