package com.example.medicalsymptomprescreener.ui.input

sealed class InputUiState {
    object Idle : InputUiState()
    data class Listening(val partialTranscript: String = "") : InputUiState()
    object Processing : InputUiState()
    data class Error(val message: String) : InputUiState()
    object Done : InputUiState()
}
