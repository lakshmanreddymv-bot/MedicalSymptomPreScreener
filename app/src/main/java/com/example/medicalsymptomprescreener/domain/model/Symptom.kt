package com.example.medicalsymptomprescreener.domain.model

import java.util.UUID

/**
 * Represents a single symptom submission from the user.
 *
 * Created at input time and persisted to Room via [SymptomEntity].
 * Passed to [TriageSymptomUseCase] and saved alongside [TriageResult] in [SymptomHistory].
 *
 * S: Single Responsibility — models one user symptom submission.
 */
data class Symptom(
    /** Unique identifier. Defaults to a random UUID. */
    val id: String = UUID.randomUUID().toString(),

    /** Free-text symptom description entered or dictated by the user. */
    val description: String,

    /** Raw speech-to-text transcript if the symptom was entered via voice. Null for text input. */
    val voiceTranscript: String? = null,

    /** Unix timestamp (milliseconds) when the symptom was submitted. */
    val timestamp: Long = System.currentTimeMillis(),

    /** Optional body area selected by the user (e.g. "Chest", "Head"). */
    val category: String? = null
)
