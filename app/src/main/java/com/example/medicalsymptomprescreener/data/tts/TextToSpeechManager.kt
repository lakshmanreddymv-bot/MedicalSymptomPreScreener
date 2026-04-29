package com.example.medicalsymptomprescreener.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/**
 * Manages Android [TextToSpeech] for safety-critical audio alerts.
 *
 * Auto-triggered by [TriageScreen] on EMERGENCY results — speaks
 * "This appears to be an emergency. Call 911 immediately." without user interaction.
 * For other urgency levels, TTS is opt-in via the speaker button in [TriageScreen].
 *
 * [init] must be called before [speak]. [destroy] must be called on screen disposal
 * (via `DisposableEffect`) to release the TTS engine.
 *
 * S: Single Responsibility — manages TTS engine lifecycle and speech output.
 */
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    /**
     * Initializes the TTS engine asynchronously.
     *
     * [onReady] is called on the main thread once the engine is ready to speak.
     * Should be called in a `DisposableEffect` in [TriageScreen] to tie lifecycle
     * to the composable's presence on the back stack.
     *
     * @param onReady Optional callback invoked when the engine initialization succeeds.
     */
    fun init(onReady: () -> Unit = {}) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isReady = true
                onReady()
            }
        }
    }

    /**
     * Speaks [text] using QUEUE_FLUSH — interrupts any currently playing speech.
     * No-op if [init] has not been called or failed.
     *
     * @param text The text to speak aloud.
     */
    fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    /** Stops the current speech without releasing the engine. */
    fun stop() {
        tts?.stop()
    }

    /**
     * Stops speech and shuts down the TTS engine, releasing all resources.
     * Must be called when the screen is disposed to prevent audio leaks.
     */
    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
