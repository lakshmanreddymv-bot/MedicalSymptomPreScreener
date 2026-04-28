package com.example.medicalsymptomprescreener.domain.model

data class NearbyFacility(
    val name: String,
    val address: String,
    val phone: String?,
    val type: CareType,
    val distanceKm: Double,
    val isOpen: Boolean?,
    val rating: Float?,
    val mapsPlaceId: String
)
