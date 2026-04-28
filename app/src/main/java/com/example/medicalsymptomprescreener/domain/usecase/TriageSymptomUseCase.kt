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

class TriageSymptomUseCase @Inject constructor(
    private val geminiApi: GeminiTriageApi,
    private val networkMonitor: NetworkMonitor
) {
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

    private fun emergencyResult() = TriageResult(
        urgencyLevel = UrgencyLevel.EMERGENCY,
        reasoning = "Your symptoms require immediate emergency care. Call 911 now.",
        recommendedCareType = CareType.EMERGENCY_ROOM,
        timeframe = "immediately",
        disclaimer = DISCLAIMER,
        followUpQuestions = emptyList()
    )

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
