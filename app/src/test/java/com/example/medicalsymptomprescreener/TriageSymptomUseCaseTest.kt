package com.example.medicalsymptomprescreener

import com.example.medicalsymptomprescreener.data.api.GeminiTriageApi
import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel
import com.example.medicalsymptomprescreener.domain.monitor.NetworkMonitor
import com.example.medicalsymptomprescreener.domain.usecase.TriageSymptomUseCase
import com.google.gson.JsonParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.EOFException
import java.io.IOException

class TriageSymptomUseCaseTest {

    private lateinit var geminiApi: GeminiTriageApi
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var useCase: TriageSymptomUseCase

    private fun fakeTriageResult(urgency: UrgencyLevel = UrgencyLevel.NON_URGENT) = TriageResult(
        urgencyLevel = urgency,
        reasoning = "Some reasoning",
        recommendedCareType = CareType.PRIMARY_CARE,
        timeframe = "soon",
        disclaimer = "not a diagnosis",
        followUpQuestions = emptyList()
    )

    // doAnswer is required for checked exceptions on Kotlin suspend mocks
    private suspend fun GeminiTriageApi.stubThrow(exception: Throwable) {
        doAnswer { throw exception }.whenever(this).triage(any())
    }

    @Before
    fun setUp() {
        geminiApi = mock()
        networkMonitor = mock()
        whenever(networkMonitor.isConnected).thenReturn(MutableStateFlow(true))
        whenever(networkMonitor.isConnectedNow()).thenReturn(true)
        useCase = TriageSymptomUseCase(geminiApi, networkMonitor)
    }

    // ── Failure Mode #1: Gemini API down ────────────────────────────────────

    @Test
    fun `FM1 Gemini IOException returns URGENT safetyFallbackResult not a crash`() = runTest {
        doAnswer { throw IOException("Connection refused") }.whenever(geminiApi).triage(any())
        val result = useCase.triage("I have a mild headache").getOrThrow()
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
        assertTrue(result.reasoning.contains("call 911", ignoreCase = true))
    }

    @Test
    fun `FM1 Gemini JsonParseException returns URGENT safetyFallbackResult`() = runTest {
        doAnswer { throw JsonParseException("bad json") }.whenever(geminiApi).triage(any())
        val result = useCase.triage("I have a mild headache").getOrThrow()
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun `FM1 Gemini EOFException truncated response returns URGENT safetyFallbackResult`() = runTest {
        doAnswer { throw EOFException("truncated") }.whenever(geminiApi).triage(any())
        val result = useCase.triage("I have a mild headache").getOrThrow()
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun `FM1 Gemini generic exception returns URGENT safetyFallbackResult`() = runTest {
        doAnswer { throw RuntimeException("server error") }.whenever(geminiApi).triage(any())
        val result = useCase.triage("stomach pain").getOrThrow()
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun `FM1 Gemini down safetyFallbackResult has URGENT_CARE as recommendedCareType`() = runTest {
        doAnswer { throw IOException("down") }.whenever(geminiApi).triage(any())
        val result = useCase.triage("mild cold").getOrThrow()
        assertEquals(CareType.URGENT_CARE, result.recommendedCareType)
    }

    @Test
    fun `FM1 Gemini down result always contains disclaimer`() = runTest {
        doAnswer { throw IOException("down") }.whenever(geminiApi).triage(any())
        val result = useCase.triage("sore throat").getOrThrow()
        assertTrue(result.disclaimer.contains("not a medical diagnosis", ignoreCase = true))
    }

    // ── Failure Mode #4: Network unavailable ────────────────────────────────

    @Test
    fun `FM4 network offline returns URGENT safetyFallbackResult without calling Gemini`() = runTest {
        whenever(networkMonitor.isConnectedNow()).thenReturn(false)
        val result = useCase.triage("mild headache").getOrThrow()
        assertEquals(UrgencyLevel.URGENT, result.urgencyLevel)
    }

    @Test
    fun `FM4 network offline result contains 911 guidance`() = runTest {
        whenever(networkMonitor.isConnectedNow()).thenReturn(false)
        val result = useCase.triage("feeling unwell").getOrThrow()
        assertTrue(result.reasoning.contains("call 911", ignoreCase = true))
    }

    @Test
    fun `FM4 network offline with emergency keywords still returns EMERGENCY - Layer 1 runs first`() = runTest {
        whenever(networkMonitor.isConnectedNow()).thenReturn(false)
        // Layer 1 runs BEFORE the network check — emergency keywords always caught
        val result = useCase.triage("I have chest pain").getOrThrow()
        assertEquals(UrgencyLevel.EMERGENCY, result.urgencyLevel)
    }

    // ── Critical safety invariant: Layer 1 runs on FULL string ──────────────

    @Test
    fun `emergency keyword beyond character 1000 still triggers EMERGENCY - full string check`() = runTest {
        // Build a string where "chest pain" appears AFTER 1000 characters
        val padding = "I feel generally unwell. ".repeat(50) // ~1250 chars
        val symptoms = padding + "chest pain"
        assertTrue("Test setup: symptoms must exceed 1000 chars", symptoms.length > 1000)
        // Layer 1 runs on FULL string — this must be caught even beyond the 1000-char truncation point
        val result = useCase.triage(symptoms).getOrThrow()
        assertEquals(UrgencyLevel.EMERGENCY, result.urgencyLevel)
    }

    // ── Happy path ──────────────────────────────────────────────────────────

    @Test
    fun `successful Gemini call with NON_URGENT result returns NON_URGENT`() = runTest {
        whenever(geminiApi.triage(any())).thenReturn(fakeTriageResult(UrgencyLevel.NON_URGENT))
        val result = useCase.triage("mild headache").getOrThrow()
        assertEquals(UrgencyLevel.NON_URGENT, result.urgencyLevel)
    }

    @Test
    fun `triage always returns Success even when Gemini throws - never propagates exception`() = runTest {
        doAnswer { throw IOException("network error") }.whenever(geminiApi).triage(any())
        val result = useCase.triage("mild cold")
        assertTrue(result.isSuccess)
    }
}
