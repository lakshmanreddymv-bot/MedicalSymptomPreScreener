package com.example.medicalsymptomprescreener.domain.repository

/**
 * Domain-layer contract for on-device ML Kit text translation.
 *
 * Implementations must:
 * - Download the Spanish language model on first use (on-device, no API key required)
 * - Translate English → Spanish on demand
 * - Return the original English text on any failure (graceful fallback)
 *
 * No Android framework types are exposed here — the interface is pure Kotlin
 * and fully unit-testable without an instrumented device.
 *
 * D: Dependency Inversion — callers depend on this abstraction, not [TranslationRepositoryImpl].
 * S: Single Responsibility — declares the translation contract only.
 */
interface TranslationRepository {

    /**
     * Ensures the Spanish translation model is downloaded to the device.
     * Safe to call multiple times — no-op if the model is already present.
     *
     * Should be called eagerly (e.g. on app start or when Spanish is first enabled)
     * so that subsequent [translate] calls are instant.
     *
     * @return [Result.success] when the model is ready; [Result.failure] if download fails.
     */
    suspend fun ensureModelDownloaded(): Result<Unit>

    /**
     * Translates [text] from English to Spanish using the on-device ML Kit model.
     *
     * Falls back to the original [text] on any error, so callers never need to
     * handle a failure case — the UI always has something to display.
     *
     * @param text English-language text to translate.
     * @return Spanish translation, or [text] unchanged if translation fails.
     */
    suspend fun translate(text: String): String

    /**
     * Closes the underlying ML Kit translator and releases native resources.
     * Must be called when the repository is no longer needed (e.g. app teardown).
     */
    fun close()
}
