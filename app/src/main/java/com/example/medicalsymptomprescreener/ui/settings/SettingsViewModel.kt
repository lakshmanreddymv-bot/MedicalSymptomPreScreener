package com.example.medicalsymptomprescreener.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalsymptomprescreener.data.local.LanguagePreferenceDataStore
import com.example.medicalsymptomprescreener.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Follows Unidirectional Data Flow (UDF):
 * - Events flow UP from UI via [setSpanishEnabled]
 * - State flows DOWN to UI via [isSpanishEnabled] StateFlow
 * - No direct state mutation from the UI layer
 *
 * Drives the Settings screen for the language preference toggle.
 *
 * When Spanish is first enabled, eagerly triggers [TranslationRepository.ensureModelDownloaded]
 * so the ML Kit Spanish model is ready before the user submits their next assessment.
 * The model download is fire-and-forget — errors are silently swallowed here because
 * [TranslationRepositoryImpl.translate] already falls back to English on failure.
 *
 * **Feature flag OFF by default:** [isSpanishEnabled] initialises to `false` (English)
 * until the user explicitly toggles the switch.
 *
 * S: Single Responsibility — manages the language preference for the Settings screen.
 * D: Dependency Inversion — depends on [LanguagePreferenceDataStore] and
 *    [TranslationRepository] abstractions.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languagePrefs: LanguagePreferenceDataStore,
    private val translationRepository: TranslationRepository
) : ViewModel() {

    /**
     * Current language preference.
     * `true` = Spanish enabled, `false` = English (default feature-flag-OFF state).
     * Backed by DataStore — survives process death and app restarts.
     */
    val isSpanishEnabled: StateFlow<Boolean> = languagePrefs.isSpanishEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    /**
     * Persists the new language preference and, when Spanish is being enabled for the
     * first time, triggers an eager model download so the ML Kit translator is ready.
     *
     * @param enabled `true` to switch to Spanish, `false` to revert to English.
     */
    fun setSpanishEnabled(enabled: Boolean) {
        viewModelScope.launch {
            languagePrefs.setSpanishEnabled(enabled)
            if (enabled) {
                // Fire-and-forget: ensure the Spanish model is downloaded.
                // translate() already falls back to English if the model isn't ready.
                translationRepository.ensureModelDownloaded()
            }
        }
    }
}
