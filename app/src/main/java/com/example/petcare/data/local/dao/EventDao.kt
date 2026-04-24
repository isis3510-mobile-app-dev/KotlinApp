package com.example.petcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.petcare.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    // ── Lecturas ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM events_local WHERE petId = :petId ORDER BY date ASC")
    fun getForPet(petId: String): Flow<List<EventEntity>>
    // petId ahora es String, antes era Int

    @Query("SELECT * FROM events_local WHERE petId = :petId AND pendingDelete = 0 ORDER BY date ASC")
    suspend fun getForPetSync(petId: String): List<EventEntity>

    @Query("SELECT * FROM events_local WHERE pendingDelete = 0 ORDER BY date ASC")
    suspend fun getAllSync(): List<EventEntity>

    @Query("SELECT * FROM events_local WHERE id = :id")
    suspend fun getById(id: String): EventEntity?

    @Query("SELECT * FROM events_local ORDER BY date ASC LIMIT 1")
    suspend fun getNext(): EventEntity?

    // ── Para WorkManager: qué hay pendiente de subir ──────────────────────

    @Query("SELECT * FROM events_local WHERE synced = 0 AND pendingDelete = 0")
    suspend fun getUnsynced(): List<EventEntity>

    @Query("SELECT * FROM events_local WHERE pendingDelete = 1")
    suspend fun getPendingDeletes(): List<EventEntity>

    // ── Escrituras ────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFromNfc(event: EventEntity)

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Upsert
    suspend fun upsert(event: EventEntity)

    // ── Actualizaciones de estado ─────────────────────────────────────────

    @Query("UPDATE events_local SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
    // id ahora es String

    @Query("UPDATE events_local SET pendingDelete = 1, synced = 0 WHERE id = :id")
    suspend fun markPendingDelete(id: String)

    @Query("DELETE FROM events_local WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM events_local WHERE synced = 1 AND pendingDelete = 0")
    suspend fun clearSynced()
}
