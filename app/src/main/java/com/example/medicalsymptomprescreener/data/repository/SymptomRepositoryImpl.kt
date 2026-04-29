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

/**
 * Room-backed implementation of [SymptomRepository].
 *
 * Handles the translation between the domain model ([Symptom], [TriageResult])
 * and the Room entity ([SymptomEntity]). Enums are stored as strings to survive
 * database schema migrations without conversion issues.
 *
 * S: Single Responsibility — bridges the domain model and Room persistence.
 * D: Dependency Inversion — implements [SymptomRepository] interface.
 */
class SymptomRepositoryImpl @Inject constructor(
    private val dao: SymptomDao
) : SymptomRepository {

    /**
     * Returns a live [Flow] of all history records from Room, mapped to domain models.
     * Emits whenever the `symptom_history` table changes.
     */
    override fun getAllHistory(): Flow<List<SymptomHistory>> =
        dao.getAllHistory().map { entities -> entities.map { it.toSymptomHistory() } }

    /** Converts [symptom] + [triageResult] to a [SymptomEntity] and inserts it into Room. */
    override suspend fun save(symptom: Symptom, triageResult: TriageResult) {
        dao.insert(symptom.toEntity(triageResult))
    }

    /** Deletes the history entry with [id] from Room. */
    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    /**
     * Maps a domain [Symptom] + [TriageResult] pair to a [SymptomEntity] for Room storage.
     * Enum values are stored by name (string) to decouple persistence from enum ordinals.
     */
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

    /**
     * Maps a [SymptomEntity] from Room back to a [SymptomHistory] domain model.
     * Unknown enum values (e.g. from a future migration) default to URGENT and URGENT_CARE
     * as a conservative safe fallback.
     */
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
