package com.example.medicalsymptomprescreener.domain.model

data class SymptomHistory(
    val id: String,
    val symptom: Symptom,
    val triageResult: TriageResult,
    val timestamp: Long
)
