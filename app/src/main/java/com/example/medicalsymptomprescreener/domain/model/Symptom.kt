package com.example.medicalsymptomprescreener.domain.model

import java.util.UUID

data class Symptom(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val voiceTranscript: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String? = null
)
