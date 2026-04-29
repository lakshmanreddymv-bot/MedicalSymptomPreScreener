package com.example.medicalsymptomprescreener.domain.repository

import com.example.medicalsymptomprescreener.domain.model.Symptom
import com.example.medicalsymptomprescreener.domain.model.SymptomHistory
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for symptom history persistence.
 *
 * Implemented by [SymptomRepositoryImpl] which delegates to [SymptomDao].
 * The domain layer depends only on this interface — zero Android dependencies.
 *
 * S: Single Responsibility — owns the symptom history persistence contract.
 * D: Dependency Inversion — domain depends on this abstraction, not on Room directly.
 */
interface SymptomRepository {
    /**
     * Returns a live [Flow] of all saved symptom history, ordered newest first.
     * Emits a new list whenever the underlying Room table changes.
     */
    fun getAllHistory(): Flow<List<SymptomHistory>>

    /**
     * Persists a [symptom] and its associated [triageResult] to local storage.
     * Called by [SaveSymptomUseCase] after every successful triage.
     */
    suspend fun save(symptom: Symptom, triageResult: TriageResult)

    /**
     * Deletes the history entry with the given [id].
     * Called when the user swipe-dismisses an item in [HistoryScreen].
     */
    suspend fun delete(id: String)
}
