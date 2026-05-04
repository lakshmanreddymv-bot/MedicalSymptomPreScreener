package com.example.medicalsymptomprescreener

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.medicalsymptomprescreener.data.local.LanguagePreferenceDataStore
import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.Symptom
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel
import com.example.medicalsymptomprescreener.domain.repository.TranslationRepository
import com.example.medicalsymptomprescreener.domain.usecase.SaveSymptomUseCase
import com.example.medicalsymptomprescreener.ui.triage.SharedTriageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [SharedTriageViewModel] translation behaviour.
 *
 * Covers:
 * - Spanish OFF (default): reasoning stored verbatim, no translation call
 * - Spanish ON: reasoning and follow-up questions are translated
 * - EMERGENCY urgency: translation is SKIPPED for instant display
 * - Translation fallback: if translate() returns original, it's still stored correctly
 * - clear(): resets triageResult to null
 * - History is saved with the original (untranslated) text
 *
 * These tests do NOT touch the 48 existing tests or the 3-layer safety architecture.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SharedTriageViewModelTranslationTest {

    @get:Rule
    val instantTaskExecutor = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var saveUseCase: SaveSymptomUseCase
    private lateinit var translationRepository: TranslationRepository
    private lateinit var languagePrefs: LanguagePreferenceDataStore
    private lateinit var viewModel: SharedTriageViewModel

    private val spanishEnabledFlow = MutableStateFlow(false)

    private fun fakeResult(
        urgency: UrgencyLevel = UrgencyLevel.NON_URGENT,
        reasoning: String = "Mild symptoms. Seek care within 48 hours.",
        followUpQuestions: List<String> = listOf("Any fever?", "Duration of symptoms?")
    ) = TriageResult(
        urgencyLevel = urgency,
        reasoning = reasoning,
        recommendedCareType = CareType.PRIMARY_CARE,
        timeframe = "within 48 hours",
        disclaimer = "Not a medical diagnosis. Consult a healthcare professional.",
        followUpQuestions = followUpQuestions
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        saveUseCase = mock()
        translationRepository = mock()
        languagePrefs = mock()
        whenever(languagePrefs.isSpanishEnabled).thenReturn(spanishEnabledFlow)
        viewModel = SharedTriageViewModel(saveUseCase, translationRepository, languagePrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Feature flag OFF (English default) ──────────────────────────────

    @Test
    fun `triageResult is null on init`() {
        assertNull(viewModel.triageResult.value)
    }

    @Test
    fun `isSpanishEnabled is false by default - feature flag OFF`() {
        assertFalse(viewModel.isSpanishEnabled.value)
    }

    @Test
    fun `setResult with Spanish OFF stores result verbatim without translation`() = runTest {
        spanishEnabledFlow.value = false
        val result = fakeResult()

        viewModel.setResult("mild headache", result)
        advanceUntilIdle()

        assertEquals("Mild symptoms. Seek care within 48 hours.", viewModel.triageResult.value?.reasoning)
        verify(translationRepository, never()).translate(any())
    }

    @Test
    fun `setResult with Spanish OFF preserves follow-up questions untranslated`() = runTest {
        spanishEnabledFlow.value = false
        val result = fakeResult(followUpQuestions = listOf("Any fever?", "Duration?"))

        viewModel.setResult("symptoms", result)
        advanceUntilIdle()

        assertEquals(listOf("Any fever?", "Duration?"), viewModel.triageResult.value?.followUpQuestions)
    }

    // ── Spanish ON ───────────────────────────────────────────────────────

    @Test
    fun `setResult with Spanish ON translates reasoning`() = runTest {
        spanishEnabledFlow.value = true
        val englishReasoning = "Mild symptoms. Seek care within 48 hours."
        val spanishReasoning = "Síntomas leves. Busque atención en 48 horas."

        whenever(translationRepository.translate(englishReasoning)).thenReturn(spanishReasoning)
        whenever(translationRepository.translate(any())).thenReturn("translated")

        val result = fakeResult(reasoning = englishReasoning, followUpQuestions = emptyList())
        viewModel.setResult("headache", result)
        advanceUntilIdle()

        assertEquals(spanishReasoning, viewModel.triageResult.value?.reasoning)
    }

    @Test
    fun `setResult with Spanish ON translates all follow-up questions`() = runTest {
        spanishEnabledFlow.value = true
        val questions = listOf("Any fever?", "Duration of symptoms?")

        whenever(translationRepository.translate("Mild symptoms. Seek care within 48 hours."))
            .thenReturn("translated reasoning")
        whenever(translationRepository.translate("Any fever?")).thenReturn("¿Tiene fiebre?")
        whenever(translationRepository.translate("Duration of symptoms?")).thenReturn("¿Cuánto tiempo llevan los síntomas?")

        val result = fakeResult(followUpQuestions = questions)
        viewModel.setResult("headache", result)
        advanceUntilIdle()

        val storedQuestions = viewModel.triageResult.value?.followUpQuestions
        assertEquals(listOf("¿Tiene fiebre?", "¿Cuánto tiempo llevan los síntomas?"), storedQuestions)
    }

    @Test
    fun `setResult with Spanish ON calls translate for reasoning`() = runTest {
        spanishEnabledFlow.value = true
        whenever(translationRepository.translate(any())).thenReturn("translated")

        val result = fakeResult(reasoning = "Seek care.", followUpQuestions = emptyList())
        viewModel.setResult("symptoms", result)
        advanceUntilIdle()

        verify(translationRepository).translate("Seek care.")
    }

    // ── EMERGENCY: translation SKIPPED ───────────────────────────────────

    @Test
    fun `setResult with Spanish ON and EMERGENCY urgency skips translation`() = runTest {
        spanishEnabledFlow.value = true
        val result = fakeResult(
            urgency = UrgencyLevel.EMERGENCY,
            reasoning = "Your symptoms require immediate emergency care. Call 911 now."
        )

        viewModel.setResult("chest pain", result)
        advanceUntilIdle()

        // Translation must NOT be called for EMERGENCY — instant display required
        verify(translationRepository, never()).translate(any())
        assertEquals(
            "Your symptoms require immediate emergency care. Call 911 now.",
            viewModel.triageResult.value?.reasoning
        )
    }

    @Test
    fun `EMERGENCY urgency level is never modified by translation logic`() = runTest {
        spanishEnabledFlow.value = true
        val result = fakeResult(urgency = UrgencyLevel.EMERGENCY)

        viewModel.setResult("chest pain", result)
        advanceUntilIdle()

        assertEquals(UrgencyLevel.EMERGENCY, viewModel.triageResult.value?.urgencyLevel)
    }

    // ── Fallback behaviour ───────────────────────────────────────────────

    @Test
    fun `setResult with Spanish ON falls back to English if translate returns original`() = runTest {
        spanishEnabledFlow.value = true
        val originalReasoning = "Mild symptoms."
        // Simulate fallback — translate returns the original text
        whenever(translationRepository.translate(any())).thenReturn(originalReasoning)

        val result = fakeResult(reasoning = originalReasoning, followUpQuestions = emptyList())
        viewModel.setResult("symptoms", result)
        advanceUntilIdle()

        // App still works — English text is displayed
        assertEquals(originalReasoning, viewModel.triageResult.value?.reasoning)
        assertNotNull(viewModel.triageResult.value)
    }

    // ── History persistence ──────────────────────────────────────────────

    @Test
    fun `setResult saves the ORIGINAL English result to history (not the translated one)`() = runTest {
        spanishEnabledFlow.value = true
        whenever(translationRepository.translate(any())).thenReturn("traducido")

        val originalResult = fakeResult(reasoning = "Original English reasoning.")
        viewModel.setResult("headache", originalResult)
        advanceUntilIdle()

        // SaveSymptomUseCase should be called with the ORIGINAL result, not the translated one
        verify(saveUseCase).invoke(
            Symptom(description = "headache"),
            originalResult
        )
    }

    // ── clear() ──────────────────────────────────────────────────────────

    @Test
    fun `clear() resets triageResult to null`() = runTest {
        whenever(translationRepository.translate(any())).thenReturn("translated")
        viewModel.setResult("headache", fakeResult())
        advanceUntilIdle()
        assertNotNull(viewModel.triageResult.value)

        viewModel.clear()
        assertNull(viewModel.triageResult.value)
    }
}
