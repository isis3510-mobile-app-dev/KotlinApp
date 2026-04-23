package com.example.petcare.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.petcare.data.local.dao.*
import com.example.petcare.data.local.entity.*

@Database(
    entities = [
        EventEntity::class,
        ReminderEntity::class,
        PetEntity::class,
        VaccinationEntity::class
        // TODO: David agrega sus entidades aqui
    ],

    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun reminderDao(): ReminderDao
    abstract fun petDao(): PetDao
    abstract fun vaccineDao(): VaccineDao
    // TODO: David agrega su DAO aquí

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "petcare_local.db"
                ).build().also { INSTANCE = it }
            }
    }
}