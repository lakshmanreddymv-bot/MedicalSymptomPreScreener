package com.example.medicalsymptomprescreener.domain.model

/**
 * A historical record pairing a [Symptom] submission with its [TriageResult].
 *
 * Loaded from Room via [GetSymptomHistoryUseCase] and displayed in [HistoryScreen].
 * Supports swipe-to-delete via [SymptomRepository.delete].
 *
 * S: Single Responsibility — joins a symptom and its triage result for history display.
 */
data class SymptomHistory(
    /** Unique identifier matching the original [Symptom.id]. */
    val id: String,

    /** The symptom description that was submitted. */
    val symptom: Symptom,

    /** The triage result returned for this symptom at the time of submission. */
    val triageResult: TriageResult,

    /** Unix timestamp (milliseconds) when the assessment was saved. */
    val timestamp: Long
)
