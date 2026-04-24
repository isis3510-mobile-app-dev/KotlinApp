package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.petcare.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs WHERE petId = :petId AND pendingDelete = 0 ORDER BY loggedAt DESC")
    fun getForPet(petId: String): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE petId = :petId AND pendingDelete = 0 ORDER BY loggedAt DESC")
    suspend fun getForPetSync(petId: String): List<WeightLogEntity>

    @Query("SELECT * FROM weight_logs WHERE id = :id")
    suspend fun getById(id: String): WeightLogEntity?

    @Query("SELECT * FROM weight_logs WHERE pendingSync = 1 AND pendingDelete = 0")
    suspend fun getPendingSync(): List<WeightLogEntity>

    @Query("SELECT * FROM weight_logs WHERE pendingDelete = 1")
    suspend fun getPendingDelete(): List<WeightLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WeightLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<WeightLogEntity>)

    @Update
    suspend fun update(log: WeightLogEntity)

    @Query("UPDATE weight_logs SET petId = :serverPetId WHERE petId = :localPetId")
    suspend fun moveToServerPet(localPetId: String, serverPetId: String)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteById(id: String)
}
