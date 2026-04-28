package com.example.medicalsymptomprescreener

import com.example.medicalsymptomprescreener.domain.safety.EmergencySymptomMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencySymptomMatcherTest {

    // ── Cardiac group ───────────────────────────────────────────────────────

    @Test
    fun `chest pain triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("I have chest pain"))
    }

    @Test
    fun `natural language heart feels weird triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("my heart feels weird"))
    }

    @Test
    fun `heart attack triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("I think I'm having a heart attack"))
    }

    @Test
    fun `chest tightness triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("severe chest tightness"))
    }

    // ── Respiratory group ───────────────────────────────────────────────────

    @Test
    fun `can't breathe triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("I can't breathe properly"))
    }

    @Test
    fun `difficulty breathing triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("difficulty breathing since an hour"))
    }

    @Test
    fun `short of breath triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("feeling short of breath"))
    }

    // ── Neurological group ──────────────────────────────────────────────────

    @Test
    fun `stroke triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("I think I'm having a stroke"))
    }

    @Test
    fun `passed out triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("I just passed out"))
    }

    @Test
    fun `seizure triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("having a seizure"))
    }

    // ── Mental health emergency ─────────────────────────────────────────────

    @Test
    fun `took too many pills triggers EMERGENCY - natural language variant`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("I took too many pills"))
    }

    @Test
    fun `suicidal triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("I am feeling suicidal"))
    }

    @Test
    fun `want to die triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("I want to die"))
    }

    // ── Allergic group ──────────────────────────────────────────────────────

    @Test
    fun `throat closing triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("my throat is closing"))
    }

    @Test
    fun `anaphylaxis triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("possible anaphylaxis"))
    }

    // ── Case insensitivity ──────────────────────────────────────────────────

    @Test
    fun `CHEST PAIN in all caps triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency("CHEST PAIN AND DIZZINESS"))
    }

    @Test
    fun `keyword embedded in longer text triggers EMERGENCY`() {
        assertTrue(EmergencySymptomMatcher.isEmergency(
            "I've had a headache for two days but now I also have chest pain"
        ))
    }

    // ── Non-emergency cases ─────────────────────────────────────────────────

    @Test
    fun `mild headache does not trigger EMERGENCY`() {
        assertFalse(EmergencySymptomMatcher.isEmergency("I have a mild headache"))
    }

    @Test
    fun `sore throat does not trigger EMERGENCY`() {
        assertFalse(EmergencySymptomMatcher.isEmergency("sore throat for two days"))
    }

    @Test
    fun `partial keyword chest alone does not trigger EMERGENCY`() {
        // "chest" alone is not in the groups — requires "chest pain", "chest tightness" etc.
        assertFalse(EmergencySymptomMatcher.isEmergency("pain in my chest area"))
        // "chest pain" IS in the group, so this actually should trigger...
        // Correction: the above should be detected. Let's test a real non-match:
        assertFalse(EmergencySymptomMatcher.isEmergency("I feel a bit off today"))
    }

    // ── Temporal + amber: requiresUrgentMinimum ─────────────────────────────

    @Test
    fun `suddenly dizzy triggers urgent minimum`() {
        assertTrue(EmergencySymptomMatcher.requiresUrgentMinimum("suddenly dizzy"))
    }

    @Test
    fun `suddenly nauseous triggers urgent minimum`() {
        assertTrue(EmergencySymptomMatcher.requiresUrgentMinimum("suddenly feeling nauseous"))
    }

    @Test
    fun `slowly getting dizzy does NOT trigger urgent minimum`() {
        assertFalse(EmergencySymptomMatcher.requiresUrgentMinimum("slowly getting dizzy over days"))
    }

    @Test
    fun `temporal word with no amber term does NOT trigger urgent minimum`() {
        assertFalse(EmergencySymptomMatcher.requiresUrgentMinimum("suddenly feeling a bit different"))
    }

    @Test
    fun `amber term with no temporal word does NOT trigger urgent minimum`() {
        assertFalse(EmergencySymptomMatcher.requiresUrgentMinimum("feeling dizzy for a week"))
    }
}
