package com.example.medicalsymptomprescreener.domain.usecase

import com.example.medicalsymptomprescreener.domain.model.Symptom
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.repository.SymptomRepository
import javax.inject.Inject

/**
 * Persists a symptom and its triage result to local Room storage.
 *
 * Called by [SharedTriageViewModel.setResult] after every successful triage.
 * Saving happens on the background coroutine launched in [viewModelScope] —
 * the UI is never blocked.
 *
 * S: Single Responsibility — wraps the symptom+result persistence operation.
 * D: Dependency Inversion — depends on [SymptomRepository] abstraction.
 */
class SaveSymptomUseCase @Inject constructor(
    private val repository: SymptomRepository
) {
    /**
     * Saves [symptom] and [triageResult] as a paired history record.
     *
     * @param symptom The submitted symptom description.
     * @param triageResult The validated triage result from the three-layer pipeline.
     */
    suspend operator fun invoke(symptom: Symptom, triageResult: TriageResult) {
        repository.save(symptom, triageResult)
    }
}
