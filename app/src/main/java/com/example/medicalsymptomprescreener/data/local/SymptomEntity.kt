package com.example.medicalsymptomprescreener.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptom_history")
data class SymptomEntity(
    @PrimaryKey val id: String,
    val description: String,
    val voiceTranscript: String?,
    val urgencyLevel: String,
    val recommendedCareType: String,
    val reasoning: String,
    val timestamp: Long,
    val category: String?
)
