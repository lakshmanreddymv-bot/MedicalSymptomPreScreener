package com.example.medicalsymptomprescreener.domain.model

// CRITICAL: RuleEngine uses ordinal comparisons. NEVER reorder this enum.
// EMERGENCY(0) = most urgent. SELF_CARE(3) = least urgent.
// See UrgencyLevelOrderingTest — enum reorder fails tests immediately.
enum class UrgencyLevel {
    EMERGENCY,   // ordinal 0 — go to ER now, call 911
    URGENT,      // ordinal 1 — see doctor within 24 hours
    NON_URGENT,  // ordinal 2 — home care with monitoring
    SELF_CARE    // ordinal 3 — rest and OTC remedies
}
