package com.example.medicalsymptomprescreener.data.repository

import com.example.medicalsymptomprescreener.data.local.SymptomDao
import com.example.medicalsymptomprescreener.data.local.SymptomEntity
import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.Symptom
import com.example.medicalsymptomprescreener.domain.model.SymptomHistory
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel
import com.example.medicalsymptomprescreener.domain.repository.SymptomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SymptomRepositoryImpl @Inject constructor(
    private val dao: SymptomDao
) : SymptomRepository {

    override fun getAllHistory(): Flow<List<SymptomHistory>> =
        dao.getAllHistory().map { entities -> entities.map { it.toSymptomHistory() } }

    override suspend fun save(symptom: Symptom, triageResult: TriageResult) {
        dao.insert(symptom.toEntity(triageResult))
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    private fun Symptom.toEntity(triageResult: TriageResult) = SymptomEntity(
        id = id,
        description = description,
        voiceTranscript = voiceTranscript,
        urgencyLevel = triageResult.urgencyLevel.name,
        recommendedCareType = triageResult.recommendedCareType.name,
        reasoning = triageResult.reasoning,
        timestamp = timestamp,
        category = category
    )

    private fun SymptomEntity.toSymptomHistory() = SymptomHistory(
        id = id,
        symptom = Symptom(
            id = id,
            description = description,
            voiceTranscript = voiceTranscript,
            timestamp = timestamp,
            category = category
        ),
        triageResult = TriageResult(
            urgencyLevel = runCatching { UrgencyLevel.valueOf(urgencyLevel) }.getOrDefault(UrgencyLevel.URGENT),
            reasoning = reasoning,
            recommendedCareType = runCatching { CareType.valueOf(recommendedCareType) }.getOrDefault(CareType.URGENT_CARE),
            timeframe = "",
            disclaimer = "This is not a medical diagnosis. Always consult a qualified healthcare professional.",
            followUpQuestions = emptyList()
        ),
        timestamp = timestamp
    )
}
