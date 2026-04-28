package com.example.medicalsymptomprescreener.data.repository

import com.example.medicalsymptomprescreener.data.api.GooglePlacesApiImpl
import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.NearbyFacility
import com.example.medicalsymptomprescreener.domain.repository.FacilityRepository
import javax.inject.Inject

class FacilityRepositoryImpl @Inject constructor(
    private val placesApi: GooglePlacesApiImpl
) : FacilityRepository {

    override suspend fun findNearby(
        latitude: Double,
        longitude: Double,
        careType: CareType,
        radiusMeters: Int
    ): List<NearbyFacility> {
        return placesApi.searchNearby(latitude, longitude, careType, radiusMeters)
    }
}
