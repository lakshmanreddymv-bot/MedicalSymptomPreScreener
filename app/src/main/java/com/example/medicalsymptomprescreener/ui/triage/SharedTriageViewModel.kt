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

// Scoped to the NavGraph — survives rotation, no serialization needed.
// InputScreen writes via setResult(), TriageScreen reads triageResult.
@HiltViewModel
class SharedTriageViewModel @Inject constructor(
    private val saveUseCase: SaveSymptomUseCase
) : ViewModel() {

    private val _triageResult = MutableStateFlow<TriageResult?>(null)
    val triageResult: StateFlow<TriageResult?> = _triageResult.asStateFlow()

    fun setResult(symptomText: String, result: TriageResult) {
        _triageResult.value = result
        viewModelScope.launch {
            saveUseCase(
                Symptom(description = symptomText),
                result
            )
        }
    }

    fun clear() {
        _triageResult.value = null
    }
}
