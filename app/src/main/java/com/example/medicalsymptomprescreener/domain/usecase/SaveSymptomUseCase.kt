package com.example.medicalsymptomprescreener.domain.usecase

import com.example.medicalsymptomprescreener.domain.model.Symptom
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.repository.SymptomRepository
import javax.inject.Inject

class SaveSymptomUseCase @Inject constructor(
    private val repository: SymptomRepository
) {
    suspend operator fun invoke(symptom: Symptom, triageResult: TriageResult) {
        repository.save(symptom, triageResult)
    }
}
