package com.example.medicalsymptomprescreener.ui.input

/**
 * Sealed hierarchy representing the current state of the symptom input flow.
 *
 * Observed by [InputScreen] to drive UI rendering — spinner during [Processing],
 * error banner during [Error], mic animation during [Listening].
 *
 * S: Single Responsibility — models the input screen's one-way state transitions.
 */
sealed class InputUiState {
    /** Initial state and state after a successful assessment is reset. */
    object Idle : InputUiState()

    /**
     * Voice recognition is active. [partialTranscript] is shown live in the text field
     * as the user speaks. Transitions to [Idle] when recognition completes.
     */
    data class Listening(val partialTranscript: String = "") : InputUiState()

    /** Gemini API call is in progress. Spinner and loading text are shown. */
    object Processing : InputUiState()

    /**
     * An error occurred (blank input, API failure, mic error).
     * [message] is shown in a red error banner below the submit button.
     */
    data class Error(val message: String) : InputUiState()

    /** Triage completed successfully. Triggers navigation to [TriageScreen]. */
    object Done : InputUiState()
}
