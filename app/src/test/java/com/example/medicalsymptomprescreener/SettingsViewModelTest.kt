package com.example.medicalsymptomprescreener

import com.example.medicalsymptomprescreener.data.local.LanguagePreferenceDataStore
import com.example.medicalsymptomprescreener.domain.repository.TranslationRepository
import com.example.medicalsymptomprescreener.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [SettingsViewModel].
 *
 * Verifies:
 * - Feature flag is OFF by default
 * - Enabling Spanish persists the preference and triggers model download
 * - Disabling Spanish persists the preference and does NOT trigger model download
 * - Model download is NOT triggered when switching back to English
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var languagePrefs: LanguagePreferenceDataStore
    private lateinit var translationRepository: TranslationRepository
    private lateinit var viewModel: SettingsViewModel

    private val spanishEnabledFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        languagePrefs = mock()
        translationRepository = mock()
        whenever(languagePrefs.isSpanishEnabled).thenReturn(spanishEnabledFlow)
        viewModel = SettingsViewModel(languagePrefs, translationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Feature flag OFF by default ──────────────────────────────────────

    @Test
    fun `isSpanishEnabled starts as false - feature flag OFF by default`() = runTest {
        assertFalse(viewModel.isSpanishEnabled.value)
    }

    // ── setSpanishEnabled(true) ──────────────────────────────────────────

    @Test
    fun `setSpanishEnabled(true) calls setSpanishEnabled on DataStore`() = runTest {
        viewModel.setSpanishEnabled(true)
        advanceUntilIdle()
        verify(languagePrefs).setSpanishEnabled(true)
    }

    @Test
    fun `setSpanishEnabled(true) triggers model download`() = runTest {
        whenever(translationRepository.ensureModelDownloaded()).thenReturn(Result.success(Unit))
        viewModel.setSpanishEnabled(true)
        advanceUntilIdle()
        verify(translationRepository).ensureModelDownloaded()
    }

    @Test
    fun `setSpanishEnabled(true) completes even if model download fails`() = runTest {
        whenever(translationRepository.ensureModelDownloaded())
            .thenReturn(Result.failure(Exception("Network error")))
        // Should not throw — model download failure is fire-and-forget
        viewModel.setSpanishEnabled(true)
        advanceUntilIdle()
        verify(languagePrefs).setSpanishEnabled(true)
    }

    // ── setSpanishEnabled(false) ─────────────────────────────────────────

    @Test
    fun `setSpanishEnabled(false) calls setSpanishEnabled(false) on DataStore`() = runTest {
        viewModel.setSpanishEnabled(false)
        advanceUntilIdle()
        verify(languagePrefs).setSpanishEnabled(false)
    }

    @Test
    fun `setSpanishEnabled(false) does NOT trigger model download`() = runTest {
        viewModel.setSpanishEnabled(false)
        advanceUntilIdle()
        verify(translationRepository, never()).ensureModelDownloaded()
    }

    // ── StateFlow reflects DataStore ─────────────────────────────────────

    @Test
    fun `isSpanishEnabled StateFlow reflects DataStore flow changes`() = runTest {
        spanishEnabledFlow.value = true
        advanceUntilIdle()
        assertTrue(viewModel.isSpanishEnabled.value)

        spanishEnabledFlow.value = false
        advanceUntilIdle()
        assertFalse(viewModel.isSpanishEnabled.value)
    }
}
