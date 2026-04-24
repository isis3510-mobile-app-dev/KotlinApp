package com.example.petcare.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.petcare.data.local.dao.*
import com.example.petcare.data.local.entity.*

@Database(
    entities = [
        EventEntity::class,
        ReminderEntity::class,
        PetEntity::class,
        VaccinationEntity::class,
        VaccineCatalogEntity::class,
        WeightLogEntity::class
    ],

    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun reminderDao(): ReminderDao
    abstract fun petDao(): PetDao
    abstract fun vaccineDao(): VaccineDao
    abstract fun vaccineCatalogDao(): VaccineCatalogDao
    abstract fun weightLogDao(): WeightLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "petcare_local.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build().also { INSTANCE = it }
            }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pets ADD COLUMN clientMutationId TEXT")
                db.execSQL("ALTER TABLE vaccinations ADD COLUMN vaccineName TEXT")
                db.execSQL("ALTER TABLE vaccinations ADD COLUMN clientMutationId TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weight_logs (
                        id TEXT NOT NULL PRIMARY KEY,
                        petId TEXT NOT NULL,
                        ownerId TEXT NOT NULL,
                        weight REAL NOT NULL,
                        loggedAt TEXT NOT NULL,
                        clientMutationId TEXT,
                        createdAt TEXT,
                        updatedAt TEXT,
                        pendingSync INTEGER NOT NULL DEFAULT 0,
                        pendingDelete INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(petId) REFERENCES pets(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weight_logs_petId ON weight_logs(petId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weight_logs_ownerId ON weight_logs(ownerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weight_logs_clientMutationId ON weight_logs(clientMutationId)")
            }
        }
    }
}
