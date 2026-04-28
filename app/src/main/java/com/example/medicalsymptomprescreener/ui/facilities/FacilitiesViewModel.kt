package com.example.medicalsymptomprescreener.ui.facilities

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.NearbyFacility
import com.example.medicalsymptomprescreener.domain.usecase.FindNearbyFacilitiesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FacilitiesUiState {
    object Loading : FacilitiesUiState()
    data class Success(val facilities: List<NearbyFacility>) : FacilitiesUiState()
    object Empty : FacilitiesUiState()
    data class Error(val message: String) : FacilitiesUiState()
    object SkipSearch : FacilitiesUiState()
}

@HiltViewModel
class FacilitiesViewModel @Inject constructor(
    private val findNearbyUseCase: FindNearbyFacilitiesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FacilitiesUiState>(FacilitiesUiState.Loading)
    val uiState: StateFlow<FacilitiesUiState> = _uiState.asStateFlow()

    fun loadFacilities(location: Location?, careType: CareType) {
        if (findNearbyUseCase.shouldSkipFacilitySearch(careType)) {
            _uiState.value = FacilitiesUiState.SkipSearch
            return
        }
        if (location == null) {
            _uiState.value = FacilitiesUiState.Error("Location unavailable. Use manual search.")
            return
        }
        viewModelScope.launch {
            _uiState.value = FacilitiesUiState.Loading
            findNearbyUseCase(location.latitude, location.longitude, careType).fold(
                onSuccess = { facilities ->
                    _uiState.value = if (facilities.isEmpty()) FacilitiesUiState.Empty
                    else FacilitiesUiState.Success(facilities)
                },
                onFailure = {
                    _uiState.value = FacilitiesUiState.Error("Could not load facilities. Try searching manually.")
                }
            )
        }
    }
}
