package com.example.medicalsymptomprescreener.domain.safety

import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel

/**
 * SAFETY CRITICAL: This class is part of the
 * three-layer defense-in-depth triage system.
 * Any changes must be accompanied by unit tests.
 * See [TriageRuleEngineTest] for test coverage.
 *
 * **Layer 2 (post-AI validator) of the three-layer safety pipeline.**
 * Validates Gemini's advisory output and can only ESCALATE urgency — never de-escalate.
 * EMERGENCY is always a floor.
 *
 * Implements the fix for the 2026 Mount Sinai "paradoxical safety explanation" finding:
 * AI triage systems were observed to write high-risk anatomical terms (e.g. "cardiac",
 * "respiratory") in their reasoning text while simultaneously setting `urgency_level=NON_URGENT`.
 * Check B detects this pattern by analyzing the reasoning text **independently** of the
 * urgency_level field — making the two signals orthogonal.
 *
 * **Three escalation checks (applied in order):**
 * 1. **Temporal floor** — if [EmergencySymptomMatcher.requiresUrgentMinimum] flagged the input,
 *    enforce URGENT minimum on Gemini's output.
 * 2. **Check A: Hedging language** — Gemini reasoning contains "might", "could", "cannot rule out",
 *    etc. → floor to URGENT. "Might be cardiac" while setting NON_URGENT is a known failure mode.
 * 3. **Check B: Anatomical risk terms** — Gemini reasoning contains "heart", "cardiac", "brain",
 *    "aorta", etc. → floor to URGENT regardless of `urgency_level` field (Mount Sinai fix).
 *
 * S: Single Responsibility — validates and potentially escalates Gemini's triage output.
 */
// Layer 2 of the three-layer safety system.
// Validates Gemini's advisory output.
// Iron rule: can only ESCALATE, never de-escalate. EMERGENCY is a floor.
object TriageRuleEngine {

    /**
     * Phrases indicating uncertainty in Gemini's reasoning.
     * If Gemini is uncertain but still sets a low urgency, the safe response is to escalate.
     * Example: "could be cardiac" while setting NON_URGENT → escalate to URGENT.
     */
    private val HEDGING_LANGUAGE = setOf(
        "might", "could", "possibly", "perhaps", "may be",
        "unlikely but", "it is possible", "cannot rule out",
        "worth considering", "if symptoms worsen", "monitor closely"
    )

    /**
     * Anatomical terms indicating high-risk organ systems in Gemini's reasoning text.
     *
     * This is the Mount Sinai 2026 "paradoxical safety explanation" fix.
     * The AI mentions dangerous anatomical terms in its reasoning but sets a low urgency level.
     * This check catches the contradiction by treating the **presence of the term**
     * as an escalation signal, independently of what urgency_level Gemini returned.
     *
     * Example: Gemini reasoning = "early cardiac involvement possible" + urgency_level = NON_URGENT
     * → "cardiac" is detected → escalate to URGENT.
     */
    private val ANATOMICAL_RISK_TERMS = setOf(
        "heart", "cardiac", "airway", "respiratory", "brain",
        "neurological", "spine", "spinal", "aorta", "aneurysm",
        "pulmonary", "blood pressure", "oxygen"
    )

    private const val DISCLAIMER =
        "This is not a medical diagnosis. Always consult a qualified healthcare professional."

    /**
     * Validates [geminiResult] against the three escalation rules.
     *
     * Returns a copy of [geminiResult] with [TriageResult.urgencyLevel] escalated if any
     * rule fires. The disclaimer is always stamped on the returned result.
     *
     * @param geminiResult The advisory [TriageResult] returned by Gemini 2.0 Flash.
     * @param originalSymptoms The original (untruncated) symptom string. Currently unused
     *   in validation checks but available for future rule extensions.
     * @param temporalMinimum `true` if [EmergencySymptomMatcher.requiresUrgentMinimum] fired —
     *   enforces URGENT minimum before any other check runs.
     * @return A validated [TriageResult] with urgency escalated if needed, never de-escalated.
     */
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
