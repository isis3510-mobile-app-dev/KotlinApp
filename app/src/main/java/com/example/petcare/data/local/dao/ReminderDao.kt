package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.petcare.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    // ── Lecturas ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM reminders WHERE eventId = :eventId")
    fun getForEvent(eventId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE fired = 0 ORDER BY triggerMs ASC")
    suspend fun getPendingFire(): List<ReminderEntity>

    // ── Escrituras ────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    // ── Actualizaciones de estado ─────────────────────────────────────────

    @Query("UPDATE reminders SET fired = 1 WHERE id = :id")
    suspend fun markFired(id: Int)

    // ── Limpieza ──────────────────────────────────────────────────────────

    @Query("DELETE FROM reminders WHERE fired = 1")
    suspend fun clearFired()

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: String)
}