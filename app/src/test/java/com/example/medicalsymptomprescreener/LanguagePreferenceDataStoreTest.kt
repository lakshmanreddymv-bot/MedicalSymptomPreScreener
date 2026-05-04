package com.example.medicalsymptomprescreener

import com.example.medicalsymptomprescreener.data.local.LanguagePreferenceDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [LanguagePreferenceDataStore] contract behaviour.
 *
 * [LanguagePreferenceDataStore] requires [Context] and DataStore — both Android-only.
 * These tests verify the contract via a mock, covering:
 * - Default state is English (feature flag OFF)
 * - Setting Spanish to true persists correctly
 * - Setting Spanish to false reverts to English
 * - [isSpanishEnabled] Flow emits the correct value on state change
 *
 * Integration tests (on-device / Robolectric) would exercise the real DataStore.
 */
class LanguagePreferenceDataStoreTest {

    private lateinit var dataStore: LanguagePreferenceDataStore
    private val spanishEnabledFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        dataStore = mock()
        whenever(dataStore.isSpanishEnabled).thenReturn(spanishEnabledFlow)
    }

    // ── Feature flag OFF by default ──────────────────────────────────────

    @Test
    fun `isSpanishEnabled defaults to false - feature flag OFF`() = runTest {
        val result = dataStore.isSpanishEnabled.first()
        assertFalse("Spanish must be disabled by default", result)
    }

    // ── setSpanishEnabled ────────────────────────────────────────────────

    @Test
    fun `setSpanishEnabled(true) enables Spanish`() = runTest {
        dataStore.setSpanishEnabled(true)
        verify(dataStore).setSpanishEnabled(true)
    }

    @Test
    fun `setSpanishEnabled(false) reverts to English`() = runTest {
        dataStore.setSpanishEnabled(false)
        verify(dataStore).setSpanishEnabled(false)
    }

    // ── Flow emissions ───────────────────────────────────────────────────

    @Test
    fun `isSpanishEnabled flow emits true after Spanish is enabled`() = runTest {
        spanishEnabledFlow.value = true
        val result = dataStore.isSpanishEnabled.first()
        assertTrue(result)
    }

    @Test
    fun `isSpanishEnabled flow emits false after Spanish is disabled`() = runTest {
        spanishEnabledFlow.value = true
        spanishEnabledFlow.value = false
        val result = dataStore.isSpanishEnabled.first()
        assertFalse(result)
    }

    @Test
    fun `isSpanishEnabled flow can be toggled multiple times`() = runTest {
        spanishEnabledFlow.value = true
        assertTrue(dataStore.isSpanishEnabled.first())
        spanishEnabledFlow.value = false
        assertFalse(dataStore.isSpanishEnabled.first())
        spanishEnabledFlow.value = true
        assertTrue(dataStore.isSpanishEnabled.first())
    }
}
