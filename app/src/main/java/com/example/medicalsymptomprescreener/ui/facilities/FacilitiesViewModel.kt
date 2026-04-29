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

/**
 * Sealed hierarchy representing the state of the nearby facilities search.
 *
 * [SkipSearch] is set when [careType] is TELEHEALTH or HOME_CARE — the router
 * in [MainActivity] should prevent [FacilitiesScreen] from loading for these types,
 * but [SkipSearch] acts as a safety net if the navigation guard is bypassed.
 */
sealed class FacilitiesUiState {
    /** Places API call is in progress. Shows a [CircularProgressIndicator]. */
    object Loading : FacilitiesUiState()

    /** Results returned. Shown as a [LazyColumn] of [FacilityCard] items. */
    data class Success(val facilities: List<NearbyFacility>) : FacilitiesUiState()

    /** Places API returned no results within the search radius. Shows [RuralFallbackCard]. */
    object Empty : FacilitiesUiState()

    /** Places API call failed or location was unavailable. Shows [RuralFallbackCard]. */
    data class Error(val message: String) : FacilitiesUiState()

    /** Care type is TELEHEALTH or HOME_CARE — no physical facility search needed. */
    object SkipSearch : FacilitiesUiState()
}

/**
 * Follows Unidirectional Data Flow (UDF):
 * - Events flow UP from UI via public functions ([loadFacilities])
 * - State flows DOWN to UI via [uiState] StateFlow
 * - No direct state mutation from the UI layer
 *
 * Manages the nearby facilities search using the user's GPS location and the
 * recommended [CareType] from the triage result.
 *
 * If [FindNearbyFacilitiesUseCase.shouldSkipFacilitySearch] returns true (TELEHEALTH/HOME_CARE),
 * sets [FacilitiesUiState.SkipSearch] immediately without calling the Places API.
 * Both Empty and Error states show [RuralFallbackCard] with 911 and Google Maps search links.
 *
 * S: Single Responsibility — drives the facilities search and exposes results to [FacilitiesScreen].
 * D: Dependency Inversion — depends on [FindNearbyFacilitiesUseCase] abstraction.
 */
@HiltViewModel
class FacilitiesViewModel @Inject constructor(
    private val findNearbyUseCase: FindNearbyFacilitiesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FacilitiesUiState>(FacilitiesUiState.Loading)

    /** Current search state. Observed by [FacilitiesScreen] to render the appropriate content. */
    val uiState: StateFlow<FacilitiesUiState> = _uiState.asStateFlow()

    /**
     * Triggers the nearby facility search for the given [location] and [careType].
     *
     * If [careType] is TELEHEALTH or HOME_CARE, sets [FacilitiesUiState.SkipSearch] immediately.
     * If [location] is null (GPS unavailable), sets [FacilitiesUiState.Error] immediately.
     * Otherwise calls the Places API and sets [Success], [Empty], or [Error] based on result.
     *
     * @param location Device location from FusedLocationProviderClient. Null if unavailable.
     * @param careType The recommended care type from the triage result.
     */
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
