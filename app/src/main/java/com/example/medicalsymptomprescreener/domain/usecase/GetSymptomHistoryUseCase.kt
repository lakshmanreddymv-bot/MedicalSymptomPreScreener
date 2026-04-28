package com.example.medicalsymptomprescreener.domain.usecase

import com.example.medicalsymptomprescreener.domain.model.SymptomHistory
import com.example.medicalsymptomprescreener.domain.repository.SymptomRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSymptomHistoryUseCase @Inject constructor(
    private val repository: SymptomRepository
) {
    operator fun invoke(): Flow<List<SymptomHistory>> = repository.getAllHistory()
}
