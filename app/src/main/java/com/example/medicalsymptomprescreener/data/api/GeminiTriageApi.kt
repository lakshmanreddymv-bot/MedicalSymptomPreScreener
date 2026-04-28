package com.example.medicalsymptomprescreener.data.api

import com.example.medicalsymptomprescreener.domain.model.TriageResult

interface GeminiTriageApi {
    suspend fun triage(symptoms: String): TriageResult
}
