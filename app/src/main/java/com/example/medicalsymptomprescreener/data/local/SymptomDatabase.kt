package com.example.medicalsymptomprescreener.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for local symptom history persistence.
 *
 * Single entity: [SymptomEntity] → table `symptom_history`.
 * Built in [AppModule] with `fallbackToDestructiveMigration()` — acceptable for a
 * portfolio app where clearing history on schema change is preferable to a migration.
 *
 * `exportSchema = false` suppresses the schema file requirement in CI builds.
 *
 * S: Single Responsibility — declares the Room database and exposes [SymptomDao].
 */
@Database(entities = [SymptomEntity::class], version = 1, exportSchema = false)
abstract class SymptomDatabase : RoomDatabase() {
    /** Provides access to [SymptomDao] for all symptom_history table operations. */
    abstract fun symptomDao(): SymptomDao
}
