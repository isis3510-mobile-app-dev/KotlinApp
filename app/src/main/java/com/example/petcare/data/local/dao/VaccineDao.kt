package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.petcare.data.local.entity.VaccinationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccineDao {

    @Query("SELECT * FROM vaccinations WHERE petId = :petId ORDER BY dateGiven DESC")
    fun getVaccinesForPet(petId: Int): Flow<List<VaccinationEntity>>

    @Query("SELECT * FROM vaccinations WHERE nextDueDate > :now ORDER BY nextDueDate ASC")
    suspend fun getUpcoming(now: Long = System.currentTimeMillis()): List<VaccinationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccine(vaccine: VaccinationEntity): Long

    @Update
    suspend fun updateVaccine(vaccine: VaccinationEntity)

    @Delete
    suspend fun deleteVaccine(vaccine: VaccinationEntity)
}