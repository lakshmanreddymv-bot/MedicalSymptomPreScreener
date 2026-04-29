package com.example.medicalsymptomprescreener.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing one symptom assessment record in the `symptom_history` table.
 *
 * Enum values ([urgencyLevel], [recommendedCareType]) are stored as strings by name
 * to decouple storage from enum ordinals. Mapped back to domain enums in [SymptomRepositoryImpl].
 *
 * S: Single Responsibility — models one Row in the Room symptom_history table.
 */
@Entity(tableName = "symptom_history")
data class SymptomEntity(
    /** Primary key — matches [Symptom.id]. */
    @PrimaryKey val id: String,

    /** Free-text symptom description as entered or dictated by the user. */
    val description: String,

    /** Speech-to-text transcript if the symptom was entered via voice. Null for text input. */
    val voiceTranscript: String?,

    /** [UrgencyLevel] name stored as a string (e.g. "URGENT"). */
    val urgencyLevel: String,

    /** [CareType] name stored as a string (e.g. "URGENT_CARE"). */
    val recommendedCareType: String,

    /** Triage reasoning text displayed in [HistoryScreen]. */
    val reasoning: String,

    /** Unix timestamp (milliseconds) of the assessment. Used for sort order. */
    val timestamp: Long,

    /** Optional body area selected by the user (e.g. "Chest"). */
    val category: String?
)
