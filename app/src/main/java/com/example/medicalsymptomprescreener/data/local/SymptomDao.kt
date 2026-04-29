package com.example.medicalsymptomprescreener.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for the `symptom_history` table.
 *
 * All queries are exposed through [SymptomRepositoryImpl] — no other class
 * should depend on this DAO directly. The [Flow] returned by [getAllHistory]
 * is a live observable — Room emits a new list on every table change.
 *
 * S: Single Responsibility — declares SQL operations on the symptom_history table.
 * D: Dependency Inversion — accessed only via [SymptomRepository] abstraction.
 */
@Dao
interface SymptomDao {
    /**
     * Returns all history rows ordered newest first as a live [Flow].
     * A new list is emitted whenever a row is inserted or deleted.
     */
    @Query("SELECT * FROM symptom_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SymptomEntity>>

    /**
     * Inserts or replaces a [SymptomEntity] row.
     * [OnConflictStrategy.REPLACE] ensures re-submitted identical IDs overwrite the old record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SymptomEntity)

    /**
     * Deletes the row with the given [id].
     * Called when the user swipe-dismisses an item in [HistoryScreen].
     */
    @Query("DELETE FROM symptom_history WHERE id = :id")
    suspend fun deleteById(id: String)
}
