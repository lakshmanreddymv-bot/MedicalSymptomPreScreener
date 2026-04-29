package com.example.medicalsymptomprescreener.domain.model

/**
 * Triage urgency classification returned by the three-layer safety pipeline.
 *
 * **CRITICAL — NEVER REORDER THESE ENTRIES.**
 * [TriageRuleEngine] uses ordinal comparisons to enforce escalation:
 * `level.ordinal > UrgencyLevel.URGENT.ordinal` means "less urgent than URGENT".
 * Reordering silently breaks all safety escalation logic.
 * [UrgencyLevelOrderingTest] will catch any reorder immediately.
 *
 * S: Single Responsibility — represents the output urgency classification only.
 */
// CRITICAL: RuleEngine uses ordinal comparisons. NEVER reorder this enum.
// EMERGENCY(0) = most urgent. SELF_CARE(3) = least urgent.
// See UrgencyLevelOrderingTest — enum reorder fails tests immediately.
enum class UrgencyLevel {
    /** Ordinal 0. Life-threatening. Go to ER now or call 911. */
    EMERGENCY,

    /** Ordinal 1. Serious but not immediately life-threatening. See a doctor within 24 hours. */
    URGENT,

    /** Ordinal 2. Non-urgent. Can be monitored at home with a scheduled appointment. */
    NON_URGENT,

    /** Ordinal 3. Least urgent. Rest, hydration, and OTC remedies are appropriate. */
    SELF_CARE
}
