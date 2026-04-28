package com.example.medicalsymptomprescreener

import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SAFETY-CRITICAL: This test exists to prevent accidental reordering of UrgencyLevel.
 * TriageRuleEngine uses ordinal comparisons for escalation logic.
 * If this enum is reordered, all safety escalation breaks silently at runtime.
 * This test must NEVER be deleted or modified to pass with a different ordering.
 */
class UrgencyLevelOrderingTest {

    @Test
    fun `EMERGENCY has lowest ordinal - most urgent`() {
        assertTrue(
            "EMERGENCY must be more urgent than URGENT — if this fails, RuleEngine escalation is BROKEN",
            UrgencyLevel.EMERGENCY.ordinal < UrgencyLevel.URGENT.ordinal
        )
    }

    @Test
    fun `URGENT is more urgent than NON_URGENT`() {
        assertTrue(
            "URGENT must be more urgent than NON_URGENT",
            UrgencyLevel.URGENT.ordinal < UrgencyLevel.NON_URGENT.ordinal
        )
    }

    @Test
    fun `NON_URGENT is more urgent than SELF_CARE`() {
        assertTrue(
            "NON_URGENT must be more urgent than SELF_CARE",
            UrgencyLevel.NON_URGENT.ordinal < UrgencyLevel.SELF_CARE.ordinal
        )
    }

    @Test
    fun `SELF_CARE has highest ordinal - least urgent`() {
        assertTrue(
            "SELF_CARE.ordinal must be greater than URGENT.ordinal for RuleEngine floor checks to work",
            UrgencyLevel.SELF_CARE.ordinal > UrgencyLevel.URGENT.ordinal
        )
    }

    @Test
    fun `escalation check ordinal greater than URGENT means less urgent than URGENT`() {
        // This is the exact comparison used in TriageRuleEngine.validate():
        // if (level.ordinal > UrgencyLevel.URGENT.ordinal) { level = UrgencyLevel.URGENT }
        // This test proves that NON_URGENT and SELF_CARE would be escalated, EMERGENCY would not
        assertTrue(UrgencyLevel.NON_URGENT.ordinal > UrgencyLevel.URGENT.ordinal)
        assertTrue(UrgencyLevel.SELF_CARE.ordinal > UrgencyLevel.URGENT.ordinal)
        assertTrue(UrgencyLevel.EMERGENCY.ordinal < UrgencyLevel.URGENT.ordinal)
    }

    @Test
    fun `exactly four urgency levels exist`() {
        // If a new level is added without reviewing RuleEngine ordinal logic, fail loudly
        assertTrue(
            "Expected 4 urgency levels. If you added a new level, review TriageRuleEngine ordinal comparisons.",
            UrgencyLevel.entries.size == 4
        )
    }
}
