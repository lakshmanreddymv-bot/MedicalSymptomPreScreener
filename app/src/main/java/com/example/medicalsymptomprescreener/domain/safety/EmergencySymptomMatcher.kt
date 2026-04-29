package com.example.medicalsymptomprescreener.domain.safety

/**
 * SAFETY CRITICAL: This class is part of the
 * three-layer defense-in-depth triage system.
 * Any changes must be accompanied by unit tests.
 * See [EmergencySymptomMatcherTest] for test coverage.
 *
 * **Layer 1 of the three-layer safety pipeline.** Runs offline in ~5 ms before any
 * network call. If [isEmergency] returns `true`, the caller returns an EMERGENCY
 * result immediately — Gemini is never invoked.
 *
 * Uses synonym groups rather than single keywords to catch natural language variants
 * ("my heart feels weird", "heart feels strange") that a keyword list would miss.
 * Runs on the **FULL untruncated** symptom string — the 1000-char truncation applied
 * before the Gemini call happens in [TriageSymptomUseCase], never here.
 *
 * S: Single Responsibility — detects emergency and urgent-minimum conditions from text only.
 */
// Layer 1 of the three-layer safety system.
// Runs on the FULL untruncated symptom string — never on truncated input.
// Returns EMERGENCY immediately, skipping Gemini entirely.
object EmergencySymptomMatcher {

    /**
     * Seven groups of synonyms covering the most life-threatening symptom categories.
     * A match in ANY phrase within ANY group triggers EMERGENCY.
     *
     * Groups are designed with natural language variants because users type how they speak,
     * not in clinical terms. "my heart feels weird" is as dangerous as "chest pain".
     */
    private val EMERGENCY_GROUPS = listOf(
        // Cardiac — includes natural language variants like "my heart feels weird".
        // Heart pounding or racing can indicate arrhythmia or myocardial infarction.
        setOf(
            "chest pain", "chest tightness", "chest pressure",
            "heart attack", "heart racing", "heart pounding",
            "my heart feels weird", "heart feels strange", "heart feels off"
        ),
        // Respiratory — any breathing impairment is immediately life-threatening.
        // All variants of "difficulty breathing" are included to catch incomplete phrasing.
        setOf(
            "can't breathe", "cannot breathe", "difficulty breathing",
            "trouble breathing", "hard to breathe", "short of breath",
            "shortness of breath", "not breathing", "stopped breathing"
        ),
        // Neurological — covers FAST stroke signs (Face drooping, Arm weakness,
        // Speech difficulty, Time to call 911) plus seizure variants.
        setOf(
            "stroke", "facial drooping", "arm weakness", "slurred speech",
            "sudden confusion", "loss of consciousness", "unconscious",
            "passed out", "seizure", "convulsing", "shaking uncontrollably",
            "paralysis", "can't move", "face drooping"
        ),
        // Severe bleeding — uncontrolled blood loss is rapidly fatal.
        setOf(
            "severe bleeding", "bleeding won't stop", "blood everywhere",
            "spurting blood", "gushing blood", "uncontrolled bleeding"
        ),
        // Allergic emergency — anaphylaxis can cause airway closure within minutes.
        // "throat is closing" included in addition to "throat closing" — substring matching
        // requires both forms (bug caught in test: "my throat is closing" missed original list).
        setOf(
            "severe allergic reaction", "anaphylaxis", "throat closing",
            "throat is closing", "throat feels like it's closing",
            "throat tightening", "can't swallow", "face swelling",
            "tongue swelling", "lips swelling"
        ),
        // Mental health emergency — suicidal ideation and overdose are medical emergencies.
        setOf(
            "suicidal", "want to die", "kill myself", "end my life",
            "overdose", "took too many pills", "poisoning", "i want to end it"
        ),
        // Other critical — choking, cyanosis (turning blue), sudden collapse.
        setOf(
            "choking", "can't speak", "turning blue", "cyanosis",
            "fainting", "collapsing", "collapsed"
        )
    )

    /**
     * Amber terms that, when combined with a [TEMPORAL_URGENT_PHRASES] phrase, trigger
     * a minimum URGENT floor. Each term individually can represent a concerning symptom.
     * "suddenly nauseous" can precede cardiac events — hence including "nauseous" and "nausea".
     */
    private val AMBER_TERMS = setOf(
        "fever", "vomiting", "nausea", "nauseous", "dizzy", "dizziness",
        "confusion", "confused", "sweating", "pale", "weak", "weakness",
        "severe", "extreme", "worst"
    )

    /**
     * Temporal onset phrases. "Suddenly dizzy" or "came out of nowhere" combined with
     * an amber term (e.g. "weakness") can indicate a stroke or TIA onset.
     * Conservative design: false positives (over-triage) are preferable to missed emergencies.
     */
    private val TEMPORAL_URGENT_PHRASES = setOf(
        "sudden", "suddenly", "all of a sudden", "just started",
        "came out of nowhere", "out of nowhere", "rapidly", "getting worse",
        "worsening fast"
    )

    /**
     * Returns `true` if [symptoms] contains any phrase from any [EMERGENCY_GROUPS] group.
     *
     * Must be called on the **full untruncated** symptom string. If this returns `true`,
     * [TriageSymptomUseCase] returns an EMERGENCY result immediately without calling Gemini.
     *
     * @param symptoms The raw symptom string as entered/dictated by the user.
     */
    fun isEmergency(symptoms: String): Boolean {
        val lower = symptoms.lowercase()
        return EMERGENCY_GROUPS.any { group -> group.any { lower.contains(it) } }
    }

    /**
     * Returns `true` if [symptoms] contains both a temporal onset phrase and an amber term.
     *
     * When `true`, [TriageRuleEngine] enforces a minimum URGENT floor on Gemini's output.
     * This catches "suddenly dizzy" — which alone might appear mild but can indicate
     * a stroke, TIA, or arrhythmia onset.
     *
     * Must be called on the **full untruncated** symptom string.
     *
     * @param symptoms The raw symptom string as entered/dictated by the user.
     */
    fun requiresUrgentMinimum(symptoms: String): Boolean {
        val lower = symptoms.lowercase()
        val hasTemporal = TEMPORAL_URGENT_PHRASES.any { lower.contains(it) }
        val hasAmber = AMBER_TERMS.any { lower.contains(it) }
        return hasTemporal && hasAmber
    }
}
