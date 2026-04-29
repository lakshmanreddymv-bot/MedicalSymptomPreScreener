package com.example.medicalsymptomprescreener.domain.repository

import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.NearbyFacility

/**
 * Domain-layer contract for nearby medical facility lookups.
 *
 * Implemented by [FacilityRepositoryImpl] which delegates to [GooglePlacesApiImpl].
 * Only called when [FindNearbyFacilitiesUseCase.shouldSkipFacilitySearch] returns false
 * (i.e. not TELEHEALTH or HOME_CARE).
 *
 * S: Single Responsibility — owns the nearby facility search contract.
 * D: Dependency Inversion — domain depends on this abstraction, not on Google Places directly.
 */
interface FacilityRepository {
    /**
     * Searches for nearby medical facilities matching [careType] within [radiusMeters].
     *
     * @param latitude User's current latitude.
     * @param longitude User's current longitude.
     * @param careType Determines which Places API types to include in the search.
     * @param radiusMeters Search radius in metres (default 10 km).
     * @return List of matching [NearbyFacility] results, sorted by the Places API.
     */
    suspend fun findNearby(
        latitude: Double,
        longitude: Double,
        careType: CareType,
        radiusMeters: Int = 10_000
    ): List<NearbyFacility>
}
