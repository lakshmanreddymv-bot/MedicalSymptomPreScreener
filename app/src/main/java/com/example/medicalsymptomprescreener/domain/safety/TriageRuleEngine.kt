package com.example.medicalsymptomprescreener.domain.safety

import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel

// Layer 2 of the three-layer safety system.
// Validates Gemini's advisory output.
// Iron rule: can only ESCALATE, never de-escalate. EMERGENCY is a floor.
object TriageRuleEngine {

    private val HEDGING_LANGUAGE = setOf(
        "might", "could", "possibly", "perhaps", "may be",
        "unlikely but", "it is possible", "cannot rule out",
        "worth considering", "if symptoms worsen", "monitor closely"
    )

    // Anatomical risk terms: fix for the 2026 Mount Sinai "paradoxical safety explanation" finding.
    // AI triage systems write danger terms in their reasoning but still set urgency_level=NON_URGENT.
    // This check catches that pattern by analyzing the reasoning text INDEPENDENTLY of urgency_level.
    private val ANATOMICAL_RISK_TERMS = setOf(
        "heart", "cardiac", "airway", "respiratory", "brain",
        "neurological", "spine", "spinal", "aorta", "aneurysm",
        "pulmonary", "blood pressure", "oxygen"
    )

    private const val DISCLAIMER =
        "This is not a medical diagnosis. Always consult a qualified healthcare professional."

    fun validate(
        geminiResult: TriageResult,
        originalSymptoms: String,
        temporalMinimum: Boolean
    ): TriageResult {
        var level = geminiResult.urgencyLevel

        // Floor from Layer 1 temporal detection
        if (temporalMinimum && level.ordinal > UrgencyLevel.URGENT.ordinal) {
            level = UrgencyLevel.URGENT
        }

        val reasoning = geminiResult.reasoning.lowercase()

        // Check A: hedging language in Gemini's reasoning
        // Escalate to URGENT floor — "might be cardiac" while setting NON_URGENT is a failure mode
        val hasHedging = HEDGING_LANGUAGE.any { reasoning.contains(it) }
        if (hasHedging && level.ordinal > UrgencyLevel.URGENT.ordinal) {
            level = UrgencyLevel.URGENT
        }

        // Check B: anatomical risk terms in Gemini's reasoning (Mount Sinai fix)
        // Applied INDEPENDENTLY of urgency_level field — this is the key
        val hasAnatomicalRisk = ANATOMICAL_RISK_TERMS.any { reasoning.contains(it) }
        if (hasAnatomicalRisk && level.ordinal > UrgencyLevel.URGENT.ordinal) {
            level = UrgencyLevel.URGENT
        }

        return geminiResult.copy(
            urgencyLevel = level,
            disclaimer = DISCLAIMER
        )
    }
}
