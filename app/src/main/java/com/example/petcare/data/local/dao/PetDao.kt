package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.petcare.data.local.entity.PetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets WHERE owner = :userId AND pendingDelete= 0 ")
    fun getAllPets(userId: String): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE owner = :userId and id = :id")
    suspend fun getPetById(userId: String, id: String): PetEntity?

    @Query("SELECT * FROM pets WHERE pendingSync = 1 AND pendingDelete = 0")
    suspend fun getPendingSync(): List<PetEntity>

    @Query("SELECT * FROM pets WHERE pendingDelete = 1")
    suspend fun getPendingDelete(): List<PetEntity>

    @Query("SELECT * FROM pets WHERE name LIKE '%' || :query || '%' AND pendingDelete = 0 AND owner = :userId")
    suspend fun search(query: String, userId: String): List<PetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pets: List<PetEntity>)

    @Update
    suspend fun updatePet(pet: PetEntity)

    @Query("DELETE FROM pets WHERE id = :id")
    suspend fun deletePetById(id: String)

    @Query("SELECT * FROM pets WHERE owner = :userId AND pendingDelete = 0 ")
    suspend fun getAllPetsSync(userId: String): List<PetEntity>
}