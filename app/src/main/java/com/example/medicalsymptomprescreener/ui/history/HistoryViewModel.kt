package com.example.medicalsymptomprescreener.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalsymptomprescreener.domain.model.SymptomHistory
import com.example.medicalsymptomprescreener.domain.repository.SymptomRepository
import com.example.medicalsymptomprescreener.domain.usecase.GetSymptomHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getHistoryUseCase: GetSymptomHistoryUseCase,
    private val repository: SymptomRepository
) : ViewModel() {

    val history: StateFlow<List<SymptomHistory>> = getHistoryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}
