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

@HiltViewModel
class InputViewModel @Inject constructor(
    private val triageUseCase: TriageSymptomUseCase,
    private val speechManager: SpeechRecognitionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<InputUiState>(InputUiState.Idle)
    val uiState: StateFlow<InputUiState> = _uiState.asStateFlow()

    private val _symptomText = MutableStateFlow("")
    val symptomText: StateFlow<String> = _symptomText.asStateFlow()

    private val _selectedBodyPart = MutableStateFlow<String?>(null)
    val selectedBodyPart: StateFlow<String?> = _selectedBodyPart.asStateFlow()

    val speechState = speechManager.state

    fun updateSymptomText(text: String) {
        _symptomText.value = text
    }

    fun selectBodyPart(part: String?) {
        _selectedBodyPart.value = part
    }

    // Called from InputScreen onClick — AFTER permission is confirmed granted in UI
    fun startListening() {
        speechManager.startListening()
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun onSpeechResult(transcript: String) {
        _symptomText.value = transcript
        _uiState.value = InputUiState.Idle
    }

    fun onSpeechError(message: String) {
        _uiState.value = InputUiState.Error(message)
    }

    // Returns the TriageResult via callback — SharedTriageViewModel owns the result
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

    fun resetState() {
        _uiState.value = InputUiState.Idle
        _symptomText.value = ""
        _selectedBodyPart.value = null
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
