package com.example.medicalsymptomprescreener.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SymptomEntity::class], version = 1, exportSchema = false)
abstract class SymptomDatabase : RoomDatabase() {
    abstract fun symptomDao(): SymptomDao
}
