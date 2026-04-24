package com.example.petcare.data.local.dao

import androidx.room.*
import com.example.petcare.data.local.entity.VaccineCatalogEntity

@Dao
interface VaccineCatalogDao {

    @Query("SELECT * FROM vaccine_catalog ORDER BY name ASC")
    suspend fun getAll(): List<VaccineCatalogEntity>

    @Query("SELECT * FROM vaccine_catalog WHERE speciesRaw LIKE '%' || :species || '%'")
    suspend fun getForSpecies(species: String): List<VaccineCatalogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vaccines: List<VaccineCatalogEntity>)

    @Query("DELETE FROM vaccine_catalog")
    suspend fun clearAll()
}
