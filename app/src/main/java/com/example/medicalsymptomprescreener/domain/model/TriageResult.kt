package com.example.medicalsymptomprescreener.domain.model

data class TriageResult(
    val urgencyLevel: UrgencyLevel,
    val reasoning: String,
    val recommendedCareType: CareType,
    val timeframe: String,
    val disclaimer: String,
    val followUpQuestions: List<String>
)
