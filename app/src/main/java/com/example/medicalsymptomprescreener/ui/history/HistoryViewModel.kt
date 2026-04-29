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

/**
 * Follows Unidirectional Data Flow (UDF):
 * - Events flow UP from UI via public functions ([delete])
 * - State flows DOWN to UI via [history] StateFlow
 * - No direct state mutation from the UI layer
 *
 * Provides [HistoryScreen] with a live list of all saved symptom assessments
 * and handles swipe-to-delete operations.
 *
 * [history] is a [StateFlow] backed by Room's live [Flow] — any insert or delete
 * automatically emits a new list to the UI without manual refresh.
 *
 * [SharingStarted.WhileSubscribed] (5 s) means the Room observer is paused when
 * [HistoryScreen] is not visible, reducing unnecessary database reads.
 *
 * S: Single Responsibility — provides history data and delete operations to [HistoryScreen].
 * D: Dependency Inversion — depends on [GetSymptomHistoryUseCase] and [SymptomRepository].
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    getHistoryUseCase: GetSymptomHistoryUseCase,
    private val repository: SymptomRepository
) : ViewModel() {

    /**
     * Live list of all symptom history entries, newest first.
     * Emits a new list on every Room table change (insert or swipe-delete).
     */
    val history: StateFlow<List<SymptomHistory>> = getHistoryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Deletes the history entry with [id] from Room.
     * Called by [HistoryScreen] when the user swipe-dismisses an entry.
     *
     * @param id The [SymptomHistory.id] to delete.
     */
    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}
