package com.example.medicalsymptomprescreener.data.repository

import com.example.medicalsymptomprescreener.data.api.GooglePlacesApiImpl
import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.NearbyFacility
import com.example.medicalsymptomprescreener.domain.repository.FacilityRepository
import javax.inject.Inject

/**
 * Google Places-backed implementation of [FacilityRepository].
 *
 * Delegates directly to [GooglePlacesApiImpl]. The thin wrapper keeps the domain layer
 * independent of the Places API implementation details.
 *
 * S: Single Responsibility — adapts [GooglePlacesApiImpl] to the [FacilityRepository] interface.
 * D: Dependency Inversion — implements [FacilityRepository] interface.
 */
class FacilityRepositoryImpl @Inject constructor(
    private val placesApi: GooglePlacesApiImpl
) : FacilityRepository {

    /**
     * Delegates the nearby facility search to [GooglePlacesApiImpl.searchNearby].
     *
     * @param latitude User's current latitude.
     * @param longitude User's current longitude.
     * @param careType Determines the Places API types included in the search.
     * @param radiusMeters Search radius in metres.
     * @return List of [NearbyFacility] results. Empty list if none found.
     */
    override suspend fun findNearby(
        latitude: Double,
        longitude: Double,
        careType: CareType,
        radiusMeters: Int
    ): List<NearbyFacility> {
        return placesApi.searchNearby(latitude, longitude, careType, radiusMeters)
    }
}
