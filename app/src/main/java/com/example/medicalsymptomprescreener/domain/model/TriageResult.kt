package com.example.medicalsymptomprescreener.domain.model

/**
 * The validated output of the three-layer triage safety pipeline.
 *
 * Produced by [TriageSymptomUseCase] after passing through:
 * 1. [EmergencySymptomMatcher] (Layer 1 — deterministic keyword gate)
 * 2. Gemini 2.5 Flash (advisory AI layer)
 * 3. [TriageRuleEngine] (Layer 2 — post-AI validation, escalation only)
 *
 * Held in [SharedTriageViewModel] and read by [TriageScreen].
 * Persisted to Room via [SymptomRepositoryImpl].
 *
 * S: Single Responsibility — carries the final triage output only.
 */
data class TriageResult(
    /** Final urgency level after all three safety layers. EMERGENCY is always a floor. */
    val urgencyLevel: UrgencyLevel,

    /** Explanation of the triage decision. May come from Gemini or a hardcoded safety fallback. */
    val reasoning: String,

    /** Recommended care destination. Determines routing: GuidanceScreen vs. FacilitiesScreen. */
    val recommendedCareType: CareType,

    /** Suggested timeframe for seeking care (e.g. "immediately", "within 24 hours"). */
    val timeframe: String,

    /** Safety disclaimer text. Always present on every result. */
    val disclaimer: String,

    /** Optional follow-up questions from Gemini for the user to ask their doctor. */
    val followUpQuestions: List<String>
)
