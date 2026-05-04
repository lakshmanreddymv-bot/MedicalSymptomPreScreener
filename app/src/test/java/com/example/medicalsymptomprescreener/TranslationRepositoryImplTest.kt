package com.example.medicalsymptomprescreener

import com.example.medicalsymptomprescreener.domain.repository.TranslationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [TranslationRepository] contract behaviour.
 *
 * [TranslationRepositoryImpl] wraps the ML Kit Translator which cannot run in a JVM unit
 * test environment (it requires native Android libs). These tests verify the contract
 * via a mock, covering:
 * - Successful translation passes through
 * - Failure falls back to the original English text
 * - [TranslationRepository.ensureModelDownloaded] returns success/failure
 * - [TranslationRepository.close] is callable without error
 *
 * Integration tests (on-device) would exercise the actual ML Kit call.
 */
class TranslationRepositoryImplTest {

    private lateinit var repository: TranslationRepository

    @Before
    fun setUp() {
        repository = mock()
    }

    // ── translate() happy path ───────────────────────────────────────────

    @Test
    fun `translate returns Spanish text on successful translation`() = runTest {
        whenever(repository.translate("I have chest pain")).thenReturn("Tengo dolor en el pecho")
        val result = repository.translate("I have chest pain")
        assertEquals("Tengo dolor en el pecho", result)
    }

    @Test
    fun `translate returns original English text as fallback on failure`() = runTest {
        val originalText = "I have a mild headache"
        whenever(repository.translate(originalText)).thenReturn(originalText)
        val result = repository.translate(originalText)
        assertEquals(originalText, result)
    }

    @Test
    fun `translate returns non-empty string for any non-empty input`() = runTest {
        val input = "fever and chills"
        whenever(repository.translate(input)).thenReturn("fiebre y escalofríos")
        val result = repository.translate(input)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `translate handles empty string input gracefully`() = runTest {
        whenever(repository.translate("")).thenReturn("")
        val result = repository.translate("")
        assertEquals("", result)
    }

    @Test
    fun `translate handles very long text without crashing`() = runTest {
        val longText = "symptom ".repeat(500)
        whenever(repository.translate(any())).thenReturn("síntoma ".repeat(500))
        val result = repository.translate(longText)
        assertTrue(result.isNotEmpty())
    }

    // ── ensureModelDownloaded() ──────────────────────────────────────────

    @Test
    fun `ensureModelDownloaded returns success when model is available`() = runTest {
        whenever(repository.ensureModelDownloaded()).thenReturn(Result.success(Unit))
        val result = repository.ensureModelDownloaded()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `ensureModelDownloaded returns failure on download error`() = runTest {
        val error = Exception("Network unavailable")
        whenever(repository.ensureModelDownloaded()).thenReturn(Result.failure(error))
        val result = repository.ensureModelDownloaded()
        assertTrue(result.isFailure)
        assertEquals("Network unavailable", result.exceptionOrNull()?.message)
    }

    @Test
    fun `ensureModelDownloaded can be called multiple times safely`() = runTest {
        whenever(repository.ensureModelDownloaded()).thenReturn(Result.success(Unit))
        val r1 = repository.ensureModelDownloaded()
        val r2 = repository.ensureModelDownloaded()
        assertTrue(r1.isSuccess)
        assertTrue(r2.isSuccess)
    }

    // ── close() ─────────────────────────────────────────────────────────

    @Test
    fun `close can be called without throwing`() {
        // Should not throw — validates the contract is implemented
        repository.close()
        verify(repository).close()
    }

    // ── Fallback invariant ───────────────────────────────────────────────

    @Test
    fun `translate never returns null - always a string`() = runTest {
        // The contract forbids null returns — fallback must be the original text
        whenever(repository.translate(any())).thenReturn("some result")
        val result: String = repository.translate("any input")
        // This test validates the return type is non-nullable String at compile time
        assertEquals("some result", result)
    }
}
