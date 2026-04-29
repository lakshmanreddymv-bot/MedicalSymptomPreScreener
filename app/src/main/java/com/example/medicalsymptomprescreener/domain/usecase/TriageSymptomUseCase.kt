package com.example.medicalsymptomprescreener.domain.usecase

import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel
import com.example.medicalsymptomprescreener.domain.monitor.NetworkMonitor
import com.example.medicalsymptomprescreener.domain.safety.EmergencySymptomMatcher
import com.example.medicalsymptomprescreener.domain.safety.TriageRuleEngine
import com.example.medicalsymptomprescreener.data.api.GeminiTriageApi
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import java.io.EOFException
import java.io.IOException
import javax.inject.Inject

/**
 * Orchestrates the three-layer triage safety pipeline for a single symptom submission.
 *
 * **Execution order:**
 * 1. [EmergencySymptomMatcher.isEmergency] — runs on FULL untruncated string (~5 ms, offline).
 *    Returns EMERGENCY immediately if any emergency keyword group matches. No API call made.
 * 2. [EmergencySymptomMatcher.requiresUrgentMinimum] — runs on FULL untruncated string.
 *    Flags temporal onset + amber term combinations for Step 4.
 * 3. [NetworkMonitor.isConnectedNow] — if offline, returns URGENT safety fallback.
 *    Layer 1 result is still reflected in TTS/UI upstream.
 * 4. Gemini 2.0 Flash API call on `symptoms.take(1000)` — advisory AI triage.
 * 5. [TriageRuleEngine.validate] — post-AI validation, escalates if needed (never de-escalates).
 *
 * All network and parsing exceptions return [safetyFallbackResult] (URGENT + call 911 message).
 * This class has zero Android dependencies — fully unit-testable with mock interfaces.
 *
 * S: Single Responsibility — orchestrates the triage pipeline for one symptom submission.
 * D: Dependency Inversion — depends on [GeminiTriageApi] and [NetworkMonitor] abstractions.
 */
class TriageSymptomUseCase @Inject constructor(
    private val geminiApi: GeminiTriageApi,
    private val networkMonitor: NetworkMonitor
) {
    /**
     * Runs the full triage pipeline for [symptoms].
     *
     * Always returns [Result.success] — all failure modes produce a safe URGENT fallback,
     * never [Result.failure]. The caller can always safely unwrap with [Result.getOrThrow].
     *
     * @param symptoms Raw symptom description as entered or dictated by the user.
     *   Layer 1 always runs on this full string. Truncation to 1000 chars happens only
     *   before the Gemini call.
     * @return [Result.success] wrapping a [TriageResult] from the validated pipeline.
     */
    suspend fun triage(symptoms: String): Result<TriageResult> {
        // Layer 1a: Emergency gate — runs on FULL untruncated string
        if (EmergencySymptomMatcher.isEmergency(symptoms)) {
            return Result.success(emergencyResult())
        }

        // Layer 1b: Temporal minimum flag — also runs on FULL string
        val temporalMinimum = EmergencySymptomMatcher.requiresUrgentMinimum(symptoms)

        // Pre-check: offline → safe fallback (Layer 1 result still applies to TTS/UI)
        if (!networkMonitor.isConnectedNow()) {
            return Result.success(safetyFallbackResult())
        }

        // Truncation only here — AFTER Layer 1 has run on the full string
        val safeSymptoms = symptoms.take(1000)

        val geminiResult = try {
            geminiApi.triage(safeSymptoms)
        } catch (e: IOException) {
            return Result.success(safetyFallbackResult())
        } catch (e: JsonParseException) {
            return Result.success(safetyFallbackResult())
        } catch (e: JsonSyntaxException) {
            return Result.success(safetyFallbackResult())
        } catch (e: EOFException) {
            return Result.success(safetyFallbackResult())
        } catch (e: Exception) {
            return Result.success(safetyFallbackResult())
        }

        // Layer 2: RuleEngine validates Gemini output (escalate only)
        val validated = TriageRuleEngine.validate(geminiResult, symptoms, temporalMinimum)
        return Result.success(validated)
    }

    /**
     * Hardcoded EMERGENCY result for Layer 1 keyword matches.
     * Returned without any API call — deterministic, offline, immediate.
     */
    private fun emergencyResult() = TriageResult(
        urgencyLevel = UrgencyLevel.EMERGENCY,
        reasoning = "Your symptoms require immediate emergency care. Call 911 now.",
        recommendedCareType = CareType.EMERGENCY_ROOM,
        timeframe = "immediately",
        disclaimer = DISCLAIMER,
        followUpQuestions = emptyList()
    )

    /**
     * Safe fallback result returned when the network is unavailable or any API/parse
     * exception occurs. Defaults to URGENT to avoid under-triaging during outages.
     */
    private fun safetyFallbackResult() = TriageResult(
        urgencyLevel = UrgencyLevel.URGENT,
        reasoning = "Unable to assess symptoms automatically. " +
                "If you think this may be an emergency, call 911 now. " +
                "Otherwise, seek care as soon as possible.",
        recommendedCareType = CareType.URGENT_CARE,
        timeframe = "as soon as possible",
        disclaimer = DISCLAIMER,
        followUpQuestions = emptyList()
    )

    companion object {
        private const val DISCLAIMER =
            "This is not a medical diagnosis. Always consult a qualified healthcare professional."
    }
}
