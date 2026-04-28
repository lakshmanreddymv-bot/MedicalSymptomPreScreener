package com.example.medicalsymptomprescreener.domain.repository

import com.example.medicalsymptomprescreener.domain.model.Symptom
import com.example.medicalsymptomprescreener.domain.model.SymptomHistory
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import kotlinx.coroutines.flow.Flow

interface SymptomRepository {
    fun getAllHistory(): Flow<List<SymptomHistory>>
    suspend fun save(symptom: Symptom, triageResult: TriageResult)
    suspend fun delete(id: String)
}
