package com.example.medicalsymptomprescreener.data.api

import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import javax.inject.Inject

/**
 * Retrofit interface for the Gemini 2.5 Flash REST API.
 *
 * Single-turn calls only — the `system_instruction` is re-sent on every request
 * to prevent instruction drift that can occur across multi-turn sessions.
 * `responseMimeType = "application/json"` eliminates prose wrappers around the JSON body.
 *
 * S: Single Responsibility — declares the Gemini generateContent HTTP endpoint.
 */
interface GeminiRetrofitService {
    /**
     * Calls the Gemini `generateContent` endpoint.
     *
     * @param apiKey Gemini API key injected via [BuildConfig.geminiapikey].
     * @param request The full prompt including system instruction, user message, and generation config.
     */
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// ── Request models ──────────────────────────────────────────────────────────

/** Top-level request body sent to the Gemini `generateContent` endpoint. */
data class GeminiRequest(
    /** System-level instruction re-sent on every call to prevent instruction drift. */
    @SerializedName("system_instruction") val systemInstruction: GeminiContent,

    /** Single user turn containing the symptom description. */
    val contents: List<GeminiContent>,

    /** Generation configuration — temperature 0.1 for consistency, JSON mime type. */
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig
)

/** A content block containing one or more text parts. */
data class GeminiContent(
    val parts: List<GeminiPart>
)

/** A single text part within a [GeminiContent] block. */
data class GeminiPart(
    val text: String
)

/**
 * Gemini generation configuration.
 *
 * [temperature] is set to 0.1 for medical triage — high temperature causes the same
 * symptoms to produce different urgency levels on repeated calls.
 * [responseMimeType] forces JSON output, preventing prose wrappers that break parsing.
 */
data class GeminiGenerationConfig(
    /** Low temperature (0.1) ensures consistent, deterministic triage recommendations. */
    val temperature: Double = 0.1,

    /** Forces the model to return a JSON object, eliminating markdown/prose wrapping. */
    @SerializedName("responseMimeType") val responseMimeType: String = "application/json"
)

// ── Response models ─────────────────────────────────────────────────────────

/** Top-level Gemini API response containing a list of generated candidates. */
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

/** A single generated candidate containing the model's response content. */
data class GeminiCandidate(
    val content: GeminiContent?
)

// ── Triage JSON schema from Gemini ──────────────────────────────────────────

/**
 * Deserialized JSON schema returned by Gemini in response to a triage prompt.
 * All fields are nullable to handle partial or malformed responses gracefully —
 * [GeminiTriageApiImpl.mapToTriageResult] provides URGENT defaults for nulls.
 */
data class TriageJson(
    @SerializedName("urgency_level") val urgencyLevel: String?,
    val reasoning: String?,
    @SerializedName("recommended_care") val recommendedCare: String?,
    val timeframe: String?,
    @SerializedName("follow_up_questions") val followUpQuestions: List<String>?,
    val disclaimer: String?
)

// ── Implementation ──────────────────────────────────────────────────────────

/**
 * Calls Gemini 2.5 Flash and maps the JSON response to a [TriageResult].
 *
 * Implements [GeminiTriageApi]. The prompt includes a strict system instruction
 * (no diagnosis, no medications, always recommend professional consultation)
 * and requests a specific JSON schema to ensure parseable output.
 *
 * Parse and mapping failures result in a safe URGENT default — see [TriageSymptomUseCase]
 * for the full exception-handling strategy.
 *
 * S: Single Responsibility — handles the Gemini API call and response mapping.
 * D: Dependency Inversion — implements [GeminiTriageApi] interface.
 */
class GeminiTriageApiImpl @Inject constructor(
    private val service: GeminiRetrofitService,

    /** Gemini API key from [BuildConfig.geminiapikey] (secrets plugin naming). */
    private val apiKey: String,

    private val gson: Gson
) : GeminiTriageApi {

    /**
     * Sends [symptoms] to Gemini 2.5 Flash and returns a [TriageResult].
     *
     * @throws IllegalStateException if the API returns an empty or null response body.
     * @throws com.google.gson.JsonSyntaxException if the response JSON is malformed.
     */
    override suspend fun triage(symptoms: String): TriageResult {
        val prompt = buildPrompt(symptoms)
        val response = service.generateContent(apiKey, prompt)

        val text = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: throw IllegalStateException("Empty Gemini response")

        val triageJson = gson.fromJson(text, TriageJson::class.java)
        return mapToTriageResult(triageJson)
    }

    /**
     * Builds the [GeminiRequest] with system instruction and user symptom message.
     *
     * System instruction re-sent every call (single-turn design):
     * - No diagnosis
     * - No medication suggestions
     * - Always recommend professional consultation
     *
     * The JSON schema is specified inline in the user message to constrain the response format.
     */
    private fun buildPrompt(symptoms: String): GeminiRequest {
        val systemText = "You are a medical triage assistant. " +
                "You do NOT diagnose conditions. " +
                "You do NOT suggest medications. " +
                "You ALWAYS recommend professional medical consultation. " +
                "You assess ONLY symptom urgency and appropriate care level."

        val userText = "Patient describes: $symptoms.\n\n" +
                "Respond ONLY with valid JSON:\n" +
                "{\"urgency_level\": \"EMERGENCY|URGENT|NON_URGENT|SELF_CARE\", " +
                "\"reasoning\": \"string max 2 sentences\", " +
                "\"recommended_care\": \"EMERGENCY_ROOM|URGENT_CARE|PRIMARY_CARE|PHARMACY|TELEHEALTH|HOME_CARE\", " +
                "\"timeframe\": \"string\", " +
                "\"follow_up_questions\": [\"string\", \"string\"], " +
                "\"disclaimer\": \"This is not a medical diagnosis. Always consult a qualified healthcare professional.\"}"

        return GeminiRequest(
            systemInstruction = GeminiContent(listOf(GeminiPart(systemText))),
            contents = listOf(GeminiContent(listOf(GeminiPart(userText)))),
            generationConfig = GeminiGenerationConfig()
        )
    }

    /**
     * Maps a [TriageJson] deserialized from Gemini's response to a [TriageResult].
     * Unknown or null enum values default to URGENT (conservative safe default).
     */
    private fun mapToTriageResult(json: TriageJson): TriageResult {
        val urgency = runCatching {
            UrgencyLevel.valueOf(json.urgencyLevel?.uppercase() ?: "URGENT")
        }.getOrDefault(UrgencyLevel.URGENT)

        val careType = runCatching {
            CareType.valueOf(json.recommendedCare?.uppercase() ?: "URGENT_CARE")
        }.getOrDefault(CareType.URGENT_CARE)

        return TriageResult(
            urgencyLevel = urgency,
            reasoning = json.reasoning ?: "Seek medical care.",
            recommendedCareType = careType,
            timeframe = json.timeframe ?: "as soon as possible",
            disclaimer = json.disclaimer
                ?: "This is not a medical diagnosis. Always consult a qualified healthcare professional.",
            followUpQuestions = json.followUpQuestions ?: emptyList()
        )
    }
}
