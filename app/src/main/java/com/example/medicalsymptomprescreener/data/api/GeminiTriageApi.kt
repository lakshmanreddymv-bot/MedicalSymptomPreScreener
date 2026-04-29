package com.example.medicalsymptomprescreener.data.api

import com.example.medicalsymptomprescreener.domain.model.TriageResult

/**
 * Domain-adjacent contract for the Gemini triage advisory call.
 *
 * Placed in the data layer because it is inherently network-facing, but the
 * [TriageSymptomUseCase] depends on this interface rather than the implementation,
 * enabling full unit testing with mock implementations.
 *
 * Implemented by [GeminiTriageApiImpl]. All exceptions propagate to [TriageSymptomUseCase]
 * which converts them to safe URGENT fallback results.
 *
 * S: Single Responsibility — declares the Gemini advisory triage operation.
 * D: Dependency Inversion — domain use case depends on this abstraction.
 */
interface GeminiTriageApi {
    /**
     * Sends [symptoms] to Gemini 2.5 Flash and returns a raw [TriageResult].
     *
     * **Advisory only** — the result is always passed through [TriageRuleEngine.validate]
     * before being returned to the UI. Never used directly as a final triage decision.
     *
     * @param symptoms Symptom description, truncated to 1000 chars before this call.
     * @throws IOException on network failure.
     * @throws JsonSyntaxException on malformed JSON response.
     * @throws IllegalStateException on empty Gemini response body.
     */
    suspend fun triage(symptoms: String): TriageResult
}
