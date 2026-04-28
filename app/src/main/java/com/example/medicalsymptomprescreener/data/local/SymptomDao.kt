package com.example.medicalsymptomprescreener.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptom_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SymptomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SymptomEntity)

    @Query("DELETE FROM symptom_history WHERE id = :id")
    suspend fun deleteById(id: String)
}
