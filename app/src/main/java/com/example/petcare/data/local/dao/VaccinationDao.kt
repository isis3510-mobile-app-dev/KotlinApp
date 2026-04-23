package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.petcare.data.local.entity.Vaccine
import kotlinx.coroutines.flow.Flow


@Dao
interface VaccineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccine(vaccine:Vaccine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccines(vaccines: List<Vaccine>)

    @Query("SELECT * FROM vaccinations WHERE petId = :petId")
    fun getVaccinesByPet(petId: String): Flow<List<Vaccine>>

    @Query("DELETE FROM vaccinations WHERE petId = :petId")
    suspend fun deleteByPet(petId: String)

    @Query("DELETE FROM vaccinations")
    suspend fun clearVaccines()
}