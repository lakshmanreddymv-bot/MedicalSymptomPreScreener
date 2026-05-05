package com.example.medicalsymptomprescreener.ui.triage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalsymptomprescreener.data.local.LanguagePreferenceDataStore
import com.example.medicalsymptomprescreener.domain.model.Symptom
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.repository.TranslationRepository
import com.example.medicalsymptomprescreener.domain.usecase.SaveSymptomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Follows Unidirectional Data Flow (UDF):
 * - Events flow UP from UI via public functions ([setResult], [clear])
 * - State flows DOWN to UI via [triageResult] and [isSpanishEnabled] StateFlows
 * - No direct state mutation from the UI layer
 *
 * Shared ViewModel scoped to the NavGraph — survives screen navigation and configuration
 * changes without serializing [TriageResult]. [InputScreen] writes via [setResult],
 * [TriageScreen] reads [triageResult].
 *
 * **Spanish translation:** When [isSpanishEnabled] is `true`, [setResult] passes the
 * Gemini-generated fields ([TriageResult.reasoning] and [TriageResult.followUpQuestions])
 * through [TranslationRepository.translate] before storing the result. All other fields
 * are either enums or the hardcoded safety disclaimer — those are handled via
 * `res/values-es/strings.xml` for system-locale users, or by conditional UI strings for
 * the in-app toggle.
 *
 * **Emergency safety:** Translation is skipped entirely when
 * [TriageResult.urgencyLevel] == EMERGENCY — the hardcoded "Call 911 Now" and TTS
 * emergency phrase must display instantly without any async delay.
 *
 * **3-layer safety architecture is unchanged:** Translation happens AFTER the three
 * safety layers (EmergencySymptomMatcher → Gemini → TriageRuleEngine) have produced
 * their final [TriageResult]. The urgency level is never re-evaluated or modified here.
 *
 * S: Single Responsibility — holds the current triage result, applies translation, and
 *    persists it to history.
 * D: Dependency Inversion — depends on [SaveSymptomUseCase], [TranslationRepository],
 *    and [LanguagePreferenceDataStore] abstractions.
 */
// Scoped to the NavGraph — survives rotation, no serialization needed.
// InputScreen writes via setResult(), TriageScreen reads triageResult.
@HiltViewModel
class SharedTriageViewModel @Inject constructor(
    private val saveUseCase: SaveSymptomUseCase,
    private val translationRepository: TranslationRepository,
    private val languagePrefs: LanguagePreferenceDataStore
) : ViewModel() {

    private val _triageResult = MutableStateFlow<TriageResult?>(null)

    /**
     * The current [TriageResult]. Null on first launch or after process death.
     * When Spanish is enabled the [TriageResult.reasoning] and
     * [TriageResult.followUpQuestions] are already translated before emission.
     * [TriageScreen] must guard against null — see process death note in class KDoc.
     */
    val triageResult: StateFlow<TriageResult?> = _triageResult.asStateFlow()

    /**
     * Whether the user has enabled Spanish in Settings.
     * Observed by [TriageScreen] to select the correct TTS phrase and UI label variants.
     * Defaults to `false` (English) per the feature-flag-OFF-by-default requirement.
     */
    val isSpanishEnabled: StateFlow<Boolean> = languagePrefs.isSpanishEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    /**
     * Sets the triage result, optionally translating AI-generated text to Spanish,
     * and persists it to local history.
     *
     * Called by [MainActivity] after [InputScreen] produces a result.
     * Translation (when enabled) and history persistence both run in [viewModelScope]
     * on background coroutines.
     *
     * **Translation scope:** Only [TriageResult.reasoning] and
     * [TriageResult.followUpQuestions] are translated — these are the Gemini AI outputs.
     * Enum fields ([TriageResult.urgencyLevel], [TriageResult.recommendedCareType]),
     * [TriageResult.timeframe], and [TriageResult.disclaimer] are handled via string
     * resources or remain in English (safe defaults for a medical app).
     *
     * **EMERGENCY skip:** When [urgencyLevel == EMERGENCY], translation is bypassed so
     * the emergency UI appears immediately — every millisecond counts in an emergency.
     *
     * @param symptomText The raw symptom text submitted by the user.
     * @param result The validated [TriageResult] from the three-layer pipeline.
     */
    fun setResult(symptomText: String, result: TriageResult) {
        viewModelScope.launch {
            val displayResult = if (isSpanishEnabled.value &&
                result.urgencyLevel != com.example.medicalsymptomprescreener.domain.model.UrgencyLevel.EMERGENCY
            ) {
                translateResult(result)
            } else {
                result
            }
            _triageResult.value = displayResult

            // Persist the ORIGINAL (untranslated) English result to history
            // so history entries are consistent regardless of language setting.
            saveUseCase(Symptom(description = symptomText), result)
        }
    }

    /**
     * Translates the AI-generated fields of [result] from English to Spanish.
     * Falls back to the original English text for any field that fails translation
     * (see [TranslationRepository.translate] for fallback behaviour).
     */
    private suspend fun translateResult(result: TriageResult): TriageResult {
        val translatedReasoning = translationRepository.translate(result.reasoning)
        val translatedQuestions = result.followUpQuestions.map { q ->
            translationRepository.translate(q)
        }
        return result.copy(
            reasoning = translatedReasoning,
            followUpQuestions = translatedQuestions
        )
    }

    /**
     * Clears the current triage result.
     * Called when the user taps "Start New Assessment" to return to [InputScreen].
     */
    fun clear() {
        _triageResult.value = null
    }
}
