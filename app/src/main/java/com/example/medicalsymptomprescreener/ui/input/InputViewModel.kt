package com.example.medicalsymptomprescreener.ui.input

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalsymptomprescreener.data.speech.RecognitionState
import com.example.medicalsymptomprescreener.data.speech.SpeechRecognitionManager
import com.example.medicalsymptomprescreener.domain.model.Symptom
import com.example.medicalsymptomprescreener.domain.usecase.TriageSymptomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Follows Unidirectional Data Flow (UDF):
 * - Events flow UP from UI via public functions ([updateSymptomText], [startListening], [submitSymptoms], etc.)
 * - State flows DOWN to UI via [uiState] and [symptomText] StateFlows
 * - No direct state mutation from the UI layer
 *
 * Manages the symptom input screen state: text entry, voice recognition, and triage submission.
 *
 * Voice recognition is delegated to [SpeechRecognitionManager]. The ViewModel subscribes to
 * [speechState] via [LaunchedEffect] in [InputScreen] and feeds results back via [onSpeechResult].
 * This keeps the permission check in [InputScreen] (UI layer) and the state in [InputViewModel].
 *
 * The triage result is NOT stored here — it is passed upward to [SharedTriageViewModel]
 * via the [onResult] callback in [submitSymptoms]. This avoids duplicate result state.
 *
 * S: Single Responsibility — drives the input screen: text, voice, and triage submission.
 * D: Dependency Inversion — depends on [TriageSymptomUseCase] and [SpeechRecognitionManager].
 */
@HiltViewModel
class InputViewModel @Inject constructor(
    private val triageUseCase: TriageSymptomUseCase,
    private val speechManager: SpeechRecognitionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<InputUiState>(InputUiState.Idle)

    /** Current UI state. Observed by [InputScreen] to toggle spinner, error banner, etc. */
    val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    private val _symptomText = MutableStateFlow("")

    /** The current symptom text — updated by text field edits or speech recognition results. */
    val symptomText: StateFlow<String> = _symptomText.asStateFlow()

    private val _selectedBodyPart = MutableStateFlow<String?>(null)

    /** The optional body area selected via the filter chips (e.g. "Chest", "Head"). */
    val selectedBodyPart: StateFlow<String?> = _selectedBodyPart.asStateFlow()

    /**
     * Live state from [SpeechRecognitionManager]. Observed in [InputScreen] via [LaunchedEffect]
     * to sync partial transcripts into [symptomText] in real time.
     */
    val speechState = speechManager.state

    /** Updates [symptomText] with [text] as the user types or speech partial results arrive. */
    fun updateSymptomText(text: String) {
        _symptomText.value = text
    }

    /** Selects or deselects a body area. Passing the same [part] again deselects it. */
    fun selectBodyPart(part: String?) {
        _selectedBodyPart.value = part
    }

    /**
     * Starts voice recognition via [SpeechRecognitionManager].
     *
     * Only called from [InputScreen] after the `RECORD_AUDIO` permission is confirmed granted.
     * Permission handling is intentionally kept in the UI layer — ViewModels must not request permissions.
     */
    fun startListening() {
        speechManager.startListening()
    }

    /** Stops the active voice recognition session and resets to [RecognitionState.Idle]. */
    fun stopListening() {
        speechManager.stopListening()
    }

    /**
     * Called when speech recognition returns a final [transcript].
     * Copies the transcript to [symptomText] and resets state to [InputUiState.Idle].
     */
    fun onSpeechResult(transcript: String) {
        _symptomText.value = transcript
        _uiState.value = InputUiState.Idle
    }

    /**
     * Called when speech recognition reports an error.
     * Sets state to [InputUiState.Error] with the user-friendly [message] from [speechErrorToMessage].
     */
    fun onSpeechError(message: String) {
        _uiState.value = InputUiState.Error(message)
    }

    /**
     * Submits the current [symptomText] through the triage pipeline.
     *
     * Validates that text is not blank, then calls [TriageSymptomUseCase.triage].
     * On success, calls [onResult] with the [TriageResult] so [MainActivity] can
     * pass it to [SharedTriageViewModel] and navigate to [TriageScreen].
     *
     * @param onResult Callback invoked on the main thread with the successful [TriageResult].
     */
    fun submitSymptoms(onResult: (com.example.medicalsymptomprescreener.domain.model.TriageResult) -> Unit) {
        val symptoms = _symptomText.value.trim()
        if (symptoms.isBlank()) {
            _uiState.value = InputUiState.Error("Please describe your symptoms.")
            return
        }
        _uiState.value = InputUiState.Processing
        viewModelScope.launch {
            triageUseCase.triage(symptoms).fold(
                onSuccess = { result ->
                    _uiState.value = InputUiState.Done
                    onResult(result)
                },
                onFailure = {
                    _uiState.value = InputUiState.Error("Assessment failed. If urgent, call 911.")
                }
            )
        }
    }

    /**
     * Resets all state to initial values.
     * Called when the user returns to [InputScreen] after completing an assessment.
     */
    fun resetState() {
        _uiState.value = InputUiState.Idle
        _symptomText.value = ""
        _selectedBodyPart.value = null
    }

    /** Destroys the [SpeechRecognitionManager] to release native recognizer resources. */
    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
