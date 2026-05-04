package com.example.medicalsymptomprescreener.data.repository

import com.example.medicalsymptomprescreener.domain.repository.TranslationRepository
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit on-device implementation of [TranslationRepository].
 *
 * Uses the Google ML Kit Translation API to translate English → Spanish
 * entirely on the device — no network call or API key required after model download.
 *
 * **Model download:** [ensureModelDownloaded] downloads the Spanish model over Wi-Fi
 * (or any network if Wi-Fi unavailable). The ML Kit SDK caches the model on-device
 * after the first successful download; subsequent calls are instant no-ops.
 *
 * **Error handling:** All exceptions from the ML Kit callbacks are caught and surfaced
 * as [Result.failure] from [ensureModelDownloaded], or as the original English text
 * from [translate] — the caller always has a displayable string.
 *
 * **Emergency safety:** [translate] is not called for hardcoded emergency strings
 * ("Call 911 Now", TTS emergency phrase) — those are handled separately in the UI
 * layer via res/values-es/strings.xml, which Android resolves automatically.
 *
 * S: Single Responsibility — wraps ML Kit Translator with suspend/coroutine bridging.
 * D: Dependency Inversion — implements [TranslationRepository].
 */
@Singleton
class TranslationRepositoryImpl @Inject constructor() : TranslationRepository {

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.SPANISH)
        .build()

    private val translator = Translation.getClient(options)

    /**
     * Downloads the Spanish ML Kit model if not already cached on-device.
     * Uses [DownloadConditions] with no Wi-Fi requirement so the model can download
     * on mobile data — medical apps should not block on Wi-Fi availability.
     *
     * @return [Result.success] when the model is ready; [Result.failure] on download error.
     */
    override suspend fun ensureModelDownloaded(): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }

    /**
     * Translates [text] from English to Spanish on-device via ML Kit.
     *
     * Returns the original [text] unchanged on any failure — ensuring the UI always
     * has something to display even if the model is not yet downloaded or an error occurs.
     * Emergency hardcoded strings are NOT passed through this method.
     *
     * @param text English text to translate (typically Gemini AI reasoning output).
     * @return Spanish translation, or [text] on any error.
     */
    override suspend fun translate(text: String): String =
        try {
            suspendCancellableCoroutine { cont ->
                translator.translate(text)
                    .addOnSuccessListener { translated -> cont.resume(translated) }
                    .addOnFailureListener { cont.resume(text) } // graceful fallback
            }
        } catch (e: Exception) {
            text // fallback to original English on unexpected exceptions
        }

    /**
     * Closes the [translator] and releases native ML Kit resources.
     * Called by the Hilt-managed singleton when the app process ends.
     */
    override fun close() {
        translator.close()
    }
}
