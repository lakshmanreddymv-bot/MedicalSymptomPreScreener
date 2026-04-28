package com.example.medicalsymptomprescreener.domain.usecase

import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.NearbyFacility
import com.example.medicalsymptomprescreener.domain.repository.FacilityRepository
import javax.inject.Inject

class FindNearbyFacilitiesUseCase @Inject constructor(
    private val repository: FacilityRepository
) {
    fun shouldSkipFacilitySearch(careType: CareType): Boolean =
        careType == CareType.TELEHEALTH || careType == CareType.HOME_CARE

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
