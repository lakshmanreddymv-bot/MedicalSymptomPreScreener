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

// Retrofit interface for Gemini REST API
interface GeminiRetrofitService {
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// ── Request models ──────────────────────────────────────────────────────────

data class GeminiRequest(
    @SerializedName("system_instruction") val systemInstruction: GeminiContent,
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Double = 0.1,
    @SerializedName("responseMimeType") val responseMimeType: String = "application/json"
)

// ── Response models ─────────────────────────────────────────────────────────

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

// ── Triage JSON schema from Gemini ──────────────────────────────────────────

data class TriageJson(
    @SerializedName("urgency_level") val urgencyLevel: String?,
    val reasoning: String?,
    @SerializedName("recommended_care") val recommendedCare: String?,
    val timeframe: String?,
    @SerializedName("follow_up_questions") val followUpQuestions: List<String>?,
    val disclaimer: String?
)

// ── Implementation ──────────────────────────────────────────────────────────

class GeminiTriageApiImpl @Inject constructor(
    private val service: GeminiRetrofitService,
    private val apiKey: String,
    private val gson: Gson
) : GeminiTriageApi {

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
