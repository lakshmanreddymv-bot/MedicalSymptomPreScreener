package com.example.medicalsymptomprescreener.domain.usecase

import com.example.medicalsymptomprescreener.domain.model.SymptomHistory
import com.example.medicalsymptomprescreener.domain.repository.SymptomRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a live [Flow] of all saved symptom history, newest first.
 *
 * Emits a new list on every Room table change (insert or delete).
 * Collected by [HistoryViewModel] and rendered in [HistoryScreen].
 *
 * S: Single Responsibility — provides the history stream to the UI layer.
 * D: Dependency Inversion — depends on [SymptomRepository] abstraction.
 */
class GetSymptomHistoryUseCase @Inject constructor(
    private val repository: SymptomRepository
) {
    /**
     * Invokes the use case and returns the history [Flow].
     * The Flow is hot — a new emission is triggered whenever the database changes.
     */
    operator fun invoke(): Flow<List<SymptomHistory>> = repository.getAllHistory()
}
