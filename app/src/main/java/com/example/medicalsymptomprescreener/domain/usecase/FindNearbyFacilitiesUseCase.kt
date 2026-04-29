package com.example.medicalsymptomprescreener.domain.usecase

import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.NearbyFacility
import com.example.medicalsymptomprescreener.domain.repository.FacilityRepository
import javax.inject.Inject

/**
 * Finds nearby medical facilities for the recommended [CareType].
 *
 * Callers should first check [shouldSkipFacilitySearch] — if the triage result is
 * [CareType.TELEHEALTH] or [CareType.HOME_CARE], no facility search should be triggered
 * and the app navigates to [GuidanceScreen] instead.
 *
 * S: Single Responsibility — wraps the facility search with a skip-check guard.
 * D: Dependency Inversion — depends on [FacilityRepository] abstraction.
 */
class FindNearbyFacilitiesUseCase @Inject constructor(
    private val repository: FacilityRepository
) {
    /**
     * Returns `true` for care types that don't require a physical location search.
     * [CareType.TELEHEALTH] and [CareType.HOME_CARE] are virtual/home — searching
     * for a building would be misleading and produce empty results.
     *
     * @param careType The recommended care type from [TriageResult].
     */
    fun shouldSkipFacilitySearch(careType: CareType): Boolean =
        careType == CareType.TELEHEALTH || careType == CareType.HOME_CARE

    /**
     * Searches for nearby [NearbyFacility] matching [careType] at the given coordinates.
     *
     * @param latitude User's current latitude (from FusedLocationProviderClient).
     * @param longitude User's current longitude.
     * @param careType Determines the Places API types used in the search query.
     * @return [Result.success] with facilities list (possibly empty) or [Result.failure] on error.
     */
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        careType: CareType
    ): Result<List<NearbyFacility>> {
        return try {
            Result.success(repository.findNearby(latitude, longitude, careType))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
