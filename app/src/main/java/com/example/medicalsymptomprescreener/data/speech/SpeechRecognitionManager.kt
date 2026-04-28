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

sealed class RecognitionState {
    object Idle : RecognitionState()
    object Listening : RecognitionState()
    data class Partial(val transcript: String) : RecognitionState()
    data class Result(val transcript: String) : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}

// @ActivityRetainedScoped — NOT @Singleton.
// Application-scoped injection can create this off the main thread, causing crashes.
// Scoped here so it survives configuration changes but is destroyed with the activity stack.
class SpeechRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    // Must be called from the main thread
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

    fun stopListening() {
        recognizer?.stopListening()
        _state.value = RecognitionState.Idle
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        _state.value = RecognitionState.Idle
    }

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
