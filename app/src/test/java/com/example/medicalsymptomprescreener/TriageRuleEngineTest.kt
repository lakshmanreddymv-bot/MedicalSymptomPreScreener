package com.example.medicalsymptomprescreener

import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel
import com.example.medicalsymptomprescreener.domain.safety.TriageRuleEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class TriageRuleEngineTest {

    private fun geminiResult(
        urgency: UrgencyLevel,
        reasoning: String = "Patient symptoms noted.",
        careType: CareType = CareType.PRIMARY_CARE
    ) = TriageResult(
        urgencyLevel = urgency,
        reasoning = reasoning,
        recommendedCareType = careType,
        timeframe = "when convenient",
        disclaimer = "This is not a medical diagnosis.",
        followUpQuestions = emptyList()
    )

    // ── Mount Sinai fix: anatomical risk terms ──────────────────────────────

    @Test
    fun `anatomical term heart in reasoning escalates NON_URGENT to URGENT`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.NON_URGENT, "Patient may have early signs of cardiac involvement"),
            "feeling tired", false
        )
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun `anatomical term airway in reasoning escalates NON_URGENT to URGENT`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.NON_URGENT, "There may be some airway irritation present"),
            "mild cough", false
        )
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun `anatomical term respiratory in reasoning escalates NON_URGENT to URGENT`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.NON_URGENT, "Early respiratory changes may be present"),
            "shortness of breath", false
        )
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun `anatomical term in reasoning does not de-escalate EMERGENCY`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.EMERGENCY, "Cardiac emergency suspected"),
            "chest pain", false
        )
        assertEquals(UrgencyLevel.EMERGENCY, result.urgencyLevel)
    }

    @Test
    fun `anatomical term with URGENT stays URGENT - no over-escalation to EMERGENCY`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.URGENT, "Some cardiac monitoring recommended"),
            "palpitations", false
        )
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    // ── Hedging language checks ─────────────────────────────────────────────

    @Test
    fun `hedging language might escalates NON_URGENT to URGENT`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.NON_URGENT, "This might be more serious than it appears"),
            "stomach ache", false
        )
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun `hedging language could escalates SELF_CARE to URGENT`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.SELF_CARE, "Could indicate something worth monitoring"),
            "mild headache", false
        )
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun `hedging language cannot rule out escalates NON_URGENT to URGENT`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.NON_URGENT, "We cannot rule out a more serious cause"),
            "dizziness", false
        )
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun `hedging language does not affect EMERGENCY`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.EMERGENCY, "This might be a stroke — urgent care needed"),
            "facial drooping", false
        )
        assertEquals(UrgencyLevel.EMERGENCY, result.urgencyLevel)
    }

    // ── Temporal minimum flag ───────────────────────────────────────────────

    @Test
    fun `temporal minimum flag escalates NON_URGENT to URGENT`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.NON_URGENT, "Patient reports dizziness"),
            "suddenly dizzy", temporalMinimum = true
        )
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun `temporal minimum flag does not affect EMERGENCY`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.EMERGENCY, "Emergency symptoms"),
            "chest pain", temporalMinimum = true
        )
        assertEquals(UrgencyLevel.EMERGENCY, result.urgencyLevel)
    }

    @Test
    fun `temporal minimum flag with URGENT stays URGENT`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.URGENT, "Urgent care needed"),
            "suddenly dizzy", temporalMinimum = true
        )
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    // ── No escalation needed ────────────────────────────────────────────────

    @Test
    fun `no triggers - SELF_CARE stays SELF_CARE`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.SELF_CARE, "Rest and fluids recommended"),
            "mild cold", false
        )
        assertEquals(UrgencyLevel.SELF_CARE, result.urgencyLevel)
    }

    @Test
    fun `no triggers - NON_URGENT stays NON_URGENT`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.NON_URGENT, "Monitor symptoms at home"),
            "minor back pain", false
        )
        assertEquals(UrgencyLevel.NON_URGENT, result.urgencyLevel)
    }

    // ── Disclaimer always added ─────────────────────────────────────────────

    @Test
    fun `disclaimer is always present in output regardless of escalation`() {
        listOf(UrgencyLevel.EMERGENCY, UrgencyLevel.URGENT, UrgencyLevel.NON_URGENT, UrgencyLevel.SELF_CARE)
            .forEach { level ->
                val result = TriageRuleEngine.validate(
                    geminiResult(level, "Some reasoning"),
                    "some symptoms", false
                )
                assert(result.disclaimer.isNotBlank()) {
                    "Disclaimer must not be blank for urgency level $level"
                }
                assert(result.disclaimer.contains("not a medical diagnosis")) {
                    "Disclaimer must contain 'not a medical diagnosis' for $level"
                }
            }
    }

    // ── Case-insensitivity ──────────────────────────────────────────────────

    @Test
    fun `anatomical term check is case-insensitive`() {
        val result = TriageRuleEngine.validate(
            geminiResult(UrgencyLevel.NON_URGENT, "CARDIAC symptoms noted in RESPIRATORY function"),
            "symptoms", false
        )
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }
}
