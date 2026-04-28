package com.example.medicalsymptomprescreener.domain.safety

// Layer 1 of the three-layer safety system.
// Runs on the FULL untruncated symptom string — never on truncated input.
// Returns EMERGENCY immediately, skipping Gemini entirely.
object EmergencySymptomMatcher {

    private val EMERGENCY_GROUPS = listOf(
        // Cardiac
        setOf(
            "chest pain", "chest tightness", "chest pressure",
            "heart attack", "heart racing", "heart pounding",
            "my heart feels weird", "heart feels strange", "heart feels off"
        ),
        // Respiratory
        setOf(
            "can't breathe", "cannot breathe", "difficulty breathing",
            "trouble breathing", "hard to breathe", "short of breath",
            "shortness of breath", "not breathing", "stopped breathing"
        ),
        // Neurological
        setOf(
            "stroke", "facial drooping", "arm weakness", "slurred speech",
            "sudden confusion", "loss of consciousness", "unconscious",
            "passed out", "seizure", "convulsing", "shaking uncontrollably",
            "paralysis", "can't move", "face drooping"
        ),
        // Severe bleeding
        setOf(
            "severe bleeding", "bleeding won't stop", "blood everywhere",
            "spurting blood", "gushing blood", "uncontrolled bleeding"
        ),
        // Allergic emergency
        setOf(
            "severe allergic reaction", "anaphylaxis", "throat closing",
            "throat is closing", "throat feels like it's closing",
            "throat tightening", "can't swallow", "face swelling",
            "tongue swelling", "lips swelling"
        ),
        // Mental health emergency
        setOf(
            "suicidal", "want to die", "kill myself", "end my life",
            "overdose", "took too many pills", "poisoning", "i want to end it"
        ),
        // Other critical
        setOf(
            "choking", "can't speak", "turning blue", "cyanosis",
            "fainting", "collapsing", "collapsed"
        )
    )

    private val AMBER_TERMS = setOf(
        "fever", "vomiting", "nausea", "nauseous", "dizzy", "dizziness",
        "confusion", "confused", "sweating", "pale", "weak", "weakness",
        "severe", "extreme", "worst"
    )

    private val TEMPORAL_URGENT_PHRASES = setOf(
        "sudden", "suddenly", "all of a sudden", "just started",
        "came out of nowhere", "out of nowhere", "rapidly", "getting worse",
        "worsening fast"
    )

    // Runs on the FULL untruncated symptoms string.
    // Returns true → EMERGENCY, skip Gemini entirely.
    fun isEmergency(symptoms: String): Boolean {
        val lower = symptoms.lowercase()
        return EMERGENCY_GROUPS.any { group -> group.any { lower.contains(it) } }
    }

    // Returns true → minimum URGENT regardless of Gemini result.
    // temporal phrase + 1 amber term (conservative — "suddenly dizzy" can be stroke onset).
    // Runs on the FULL untruncated symptoms string.
    fun requiresUrgentMinimum(symptoms: String): Boolean {
        val lower = symptoms.lowercase()
        val hasTemporal = TEMPORAL_URGENT_PHRASES.any { lower.contains(it) }
        val hasAmber = AMBER_TERMS.any { lower.contains(it) }
        return hasTemporal && hasAmber
    }
}
