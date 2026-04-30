package com.example.medicalsymptomprescreener

import com.example.medicalsymptomprescreener.data.api.GeminiRetrofitService
import com.example.medicalsymptomprescreener.data.api.GeminiTriageApiImpl
import com.example.medicalsymptomprescreener.data.api.GeminiRequest
import com.example.medicalsymptomprescreener.data.api.GeminiResponse
import com.example.medicalsymptomprescreener.data.api.GeminiCandidate
import com.example.medicalsymptomprescreener.data.api.GeminiContent
import com.example.medicalsymptomprescreener.data.api.GeminiPart
import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests for the LRU response cache in [GeminiTriageApiImpl].
 *
 * Verifies cache hit/miss behaviour, TTL expiry, LRU eviction, and that distinct
 * symptoms bypass each other's cache entries.
 */
class GeminiCacheTest {

    private lateinit var service: GeminiRetrofitService
    private lateinit var api: GeminiTriageApiImpl
    private val gson = Gson()

    private fun makeResponse(urgency: String): GeminiResponse {
        val json = """{"urgency_level":"$urgency","reasoning":"test","recommended_care":"URGENT_CARE","timeframe":"soon","follow_up_questions":[],"disclaimer":"disclaimer"}"""
        return GeminiResponse(listOf(GeminiCandidate(GeminiContent(listOf(GeminiPart(json))))))
    }

    @Before
    fun setUp() {
        service = mock()
        api = GeminiTriageApiImpl(service, "test-key", gson)
    }

    @Test
    fun `cache hit returns cached result without calling service again`() = runTest {
        whenever(service.generateContent(any(), any())).thenReturn(makeResponse("NON_URGENT"))

        api.triage("mild headache")
        api.triage("mild headache")

        verify(service, times(1)).generateContent(any(), any())
    }

    @Test
    fun `distinct symptoms each call service independently`() = runTest {
        whenever(service.generateContent(any(), any())).thenReturn(makeResponse("NON_URGENT"))

        api.triage("mild headache")
        api.triage("sore throat")

        verify(service, times(2)).generateContent(any(), any())
    }

    @Test
    fun `cache returns correct result on hit`() = runTest {
        whenever(service.generateContent(any(), any()))
            .thenReturn(makeResponse("URGENT"))

        val first = api.triage("sudden dizziness")
        val second = api.triage("sudden dizziness")

        assertEquals(first.urgencyLevel, second.urgencyLevel)
        assertEquals(UrgencyLevel.URGENT, second.urgencyLevel)
    }

    @Test
    fun `lru eviction removes oldest entry when cache exceeds max size`() = runTest {
        whenever(service.generateContent(any(), any())).thenReturn(makeResponse("NON_URGENT"))

        // Fill cache to capacity (5 entries)
        val symptoms = listOf("symptom A", "symptom B", "symptom C", "symptom D", "symptom E")
        symptoms.forEach { api.triage(it) }

        // Adding a 6th entry should evict "symptom A" (least recently used)
        api.triage("symptom F")

        // "symptom A" should now miss the cache — service called again
        api.triage("symptom A")

        // 6 unique fills + 1 re-miss for A = 7 total service calls
        verify(service, times(7)).generateContent(any(), any())
    }

    @Test
    fun `expired entry triggers new service call`() = runTest {
        // Inject a pre-expired entry by calling the internal cache directly via reflection
        whenever(service.generateContent(any(), any())).thenReturn(makeResponse("NON_URGENT"))

        // First call populates cache
        api.triage("fever")

        // Manually expire the entry via reflection
        val cacheField = GeminiTriageApiImpl::class.java.getDeclaredField("cache")
        cacheField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val cache = cacheField.get(api) as java.util.LinkedHashMap<Int, Any>
        val key = "fever".take(GeminiTriageApiImpl.CACHE_KEY_LENGTH).hashCode()
        // Replace with an entry timestamped far in the past
        val entryClass = Class.forName("com.example.medicalsymptomprescreener.data.api.GeminiTriageApiImpl\$CacheEntry")
        val constructor = entryClass.getDeclaredConstructors().first()
        constructor.isAccessible = true
        val expiredEntry = constructor.newInstance(
            api.triage("fever").let {
                // We already called triage twice above — get result from second call
                it
            },
            System.currentTimeMillis() - (GeminiTriageApiImpl.CACHE_TTL_MS + 1_000L)
        )
        synchronized(cache) { cache[key] = expiredEntry }

        // Third call should miss and call service again
        api.triage("fever")

        // 2 cache misses (first fill + after expiry injection)
        verify(service, times(2)).generateContent(any(), any())
    }

    @Test
    fun `cache key uses first 200 chars only`() = runTest {
        whenever(service.generateContent(any(), any())).thenReturn(makeResponse("NON_URGENT"))

        val base = "a".repeat(200)
        api.triage(base + "DIFFERENT_SUFFIX_1")
        api.triage(base + "DIFFERENT_SUFFIX_2")

        // Both produce the same 200-char prefix → same cache key → one service call
        verify(service, times(1)).generateContent(any(), any())
    }
}
