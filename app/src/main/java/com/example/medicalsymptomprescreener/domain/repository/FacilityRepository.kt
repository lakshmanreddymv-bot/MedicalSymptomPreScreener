package com.example.medicalsymptomprescreener.domain.repository

import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.NearbyFacility

interface FacilityRepository {
    suspend fun findNearby(
        latitude: Double,
        longitude: Double,
        careType: CareType,
        radiusMeters: Int = 10_000
    ): List<NearbyFacility>
}
