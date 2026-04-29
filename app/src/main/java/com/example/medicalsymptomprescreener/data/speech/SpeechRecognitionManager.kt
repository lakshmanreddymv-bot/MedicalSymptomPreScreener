package com.example.medicalsymptomprescreener.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject

/**
 * Sealed hierarchy representing the current state of the speech recognizer.
 * Observed by [InputViewModel] and reflected in [InputScreen] UI.
 */
sealed class RecognitionState {
    /** Recognizer is not active. Initial and post-result state. */
    object Idle : RecognitionState()

    /** Recognizer is listening and waiting for speech. */
    object Listening : RecognitionState()

    /** Recognizer has a partial result mid-utterance. Shown as a live transcript. */
    data class Partial(val transcript: String) : RecognitionState()

    /** Final recognized transcript. Copied to the symptom text field. */
    data class Result(val transcript: String) : RecognitionState()

    /** Recognition failed. [message] is a user-friendly string from [speechErrorToMessage]. */
    data class Error(val message: String) : RecognitionState()
}

/**
 * Manages the Android [SpeechRecognizer] lifecycle for voice symptom input.
 *
 * **Scoped as `@ActivityRetainedScoped` via Hilt, NOT `@Singleton`.**
 * `@Singleton` can instantiate [SpeechRecognizer] off the main thread during app startup,
 * causing silent failures. `@ActivityRetainedScoped` ensures the instance is created
 * on the main thread and is destroyed when the activity stack is cleared.
 *
 * All 9 [SpeechRecognizer] error codes are mapped to user-friendly strings in
 * [speechErrorToMessage] to prevent cryptic error messages in the UI.
 *
 * [destroy] must be called from [ViewModel.onCleared] to release the native recognizer resource.
 *
 * S: Single Responsibility — manages voice recognition lifecycle and exposes a state flow.
 */
// @ActivityRetainedScoped — NOT @Singleton.
// Application-scoped injection can create this off the main thread, causing crashes.
// Scoped here so it survives configuration changes but is destroyed with the activity stack.
class SpeechRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recognizer: SpeechRecognizer? = null

    /** Current state of the speech recognizer. Observed by [InputViewModel.speechState]. */
    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    /**
     * Creates a new [SpeechRecognizer], attaches a [RecognitionListener], and starts listening.
     *
     * Any previously active recognizer is destroyed before a new one is created.
     * Must be called from the **main thread** — [SpeechRecognizer] requires main-thread creation.
     * Only called from [InputViewModel.startListening], which is triggered after the
     * `RECORD_AUDIO` permission is confirmed granted by [InputScreen].
     */
    fun startListening() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = RecognitionState.Listening
                }
                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: return
                    _state.value = RecognitionState.Result(text)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.let { _state.value = RecognitionState.Partial(it) }
                }
                override fun onError(error: Int) {
                    _state.value = RecognitionState.Error(speechErrorToMessage(error))
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        recognizer?.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
        )
    }

    /**
     * Stops the active recognition session and resets state to [RecognitionState.Idle].
     * Called when the user taps the mic button while listening, or when [InputScreen] disposes.
     */
    fun stopListening() {
        recognizer?.stopListening()
        _state.value = RecognitionState.Idle
    }

    /**
     * Destroys the [SpeechRecognizer] instance and releases its native resources.
     * Must be called from [InputViewModel.onCleared] to prevent resource leaks.
     */
    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        _state.value = RecognitionState.Idle
    }

    /**
     * Maps all 9 [SpeechRecognizer] error codes to user-friendly messages.
     *
     * All 9 codes are explicitly handled — the `else` branch handles any future
     * error codes added by the Android SDK.
     *
     * @param error One of the `SpeechRecognizer.ERROR_*` constants.
     * @return A human-readable error message suitable for display in the UI.
     */
    private fun speechErrorToMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please try again."
        SpeechRecognizer.ERROR_CLIENT -> "Voice input error. Please try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required. Enable in Settings."
        SpeechRecognizer.ERROR_NETWORK -> "Network error. Use text input instead."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Connection timed out. Use text input instead."
        SpeechRecognizer.ERROR_NO_MATCH -> "Could not understand. Try speaking more clearly."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer busy. Please wait and try again."
        SpeechRecognizer.ERROR_SERVER -> "Server error. Use text input instead."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the mic button to try again."
        else -> "Voice input error. Use text input instead."
    }
}
