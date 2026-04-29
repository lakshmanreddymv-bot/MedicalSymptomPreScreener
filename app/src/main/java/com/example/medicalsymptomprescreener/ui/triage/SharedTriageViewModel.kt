package com.example.medicalsymptomprescreener.ui.triage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalsymptomprescreener.domain.model.Symptom
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.usecase.SaveSymptomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Follows Unidirectional Data Flow (UDF):
 * - Events flow UP from UI via public functions ([setResult], [clear])
 * - State flows DOWN to UI via [triageResult] StateFlow
 * - No direct state mutation from the UI layer
 *
 * Shared ViewModel scoped to the NavGraph — survives screen navigation and configuration
 * changes without serializing [TriageResult]. [InputScreen] writes via [setResult],
 * [TriageScreen] reads [triageResult].
 *
 * **Why NavGraph scope?** [TriageResult] contains a `List<String>` which cannot be
 * serialized as a navigation argument. NavGraph scope keeps the result in memory
 * across [InputScreen] → [TriageScreen] → [FacilitiesScreen] without serialization.
 *
 * **Process death safety:** [triageResult] is nullable. [TriageScreen] guards with
 * `triageResult ?: run { SessionExpiredCard(); return }` — shows an explicit
 * "Session Expired" card instead of crashing or showing a blank screen.
 *
 * S: Single Responsibility — holds the current triage result and persists it to history.
 * D: Dependency Inversion — depends on [SaveSymptomUseCase] abstraction.
 */
// Scoped to the NavGraph — survives rotation, no serialization needed.
// InputScreen writes via setResult(), TriageScreen reads triageResult.
@HiltViewModel
class SharedTriageViewModel @Inject constructor(
    private val saveUseCase: SaveSymptomUseCase
) : ViewModel() {

    private val _triageResult = MutableStateFlow<TriageResult?>(null)

    /**
     * The current [TriageResult]. Null on first launch or after process death.
     * [TriageScreen] must guard against null — see process death note in class KDoc.
     */
    val triageResult: StateFlow<TriageResult?> = _triageResult.asStateFlow()

    /**
     * Sets the triage result and persists it to local history.
     *
     * Called by [MainActivity] after [InputScreen] produces a result.
     * History persistence runs in [viewModelScope] on a background coroutine.
     *
     * @param symptomText The raw symptom text submitted by the user.
     * @param result The validated [TriageResult] from the three-layer pipeline.
     */
    fun setResult(symptomText: String, result: TriageResult) {
        _triageResult.value = result
        viewModelScope.launch {
            saveUseCase(
                Symptom(description = symptomText),
                result
            )
        }
    }

    /**
     * Clears the current triage result.
     * Called when the user taps "Start New Assessment" to return to [InputScreen].
     */
    fun clear() {
        _triageResult.value = null
    }
}
