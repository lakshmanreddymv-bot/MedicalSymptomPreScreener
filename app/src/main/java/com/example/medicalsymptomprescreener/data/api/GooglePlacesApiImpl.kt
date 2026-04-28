package com.example.medicalsymptomprescreener.data.api

import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.NearbyFacility
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import javax.inject.Inject
import kotlin.math.sqrt

// Retrofit interface for Google Places API v1 (new endpoint — NOT deprecated nearbysearch)
interface GooglePlacesRetrofitService {
    @POST("v1/places:searchNearby")
    suspend fun searchNearby(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body request: PlacesNearbyRequest
    ): PlacesNearbyResponse

    companion object {
        const val FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress," +
            "places.nationalPhoneNumber,places.currentOpeningHours," +
            "places.rating,places.location,places.types"
    }
}

// ── Request models ──────────────────────────────────────────────────────────

data class PlacesNearbyRequest(
    @SerializedName("includedTypes") val includedTypes: List<String>,
    @SerializedName("locationRestriction") val locationRestriction: LocationRestriction,
    @SerializedName("maxResultCount") val maxResultCount: Int = 10
)

data class LocationRestriction(
    val circle: Circle
)

data class Circle(
    val center: LatLng,
    val radius: Double
)

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

// ── Response models ─────────────────────────────────────────────────────────

data class PlacesNearbyResponse(
    val places: List<PlaceResult>?
)

data class PlaceResult(
    val id: String?,
    @SerializedName("displayName") val displayName: DisplayName?,
    @SerializedName("formattedAddress") val formattedAddress: String?,
    @SerializedName("nationalPhoneNumber") val phone: String?,
    @SerializedName("currentOpeningHours") val openingHours: OpeningHours?,
    val rating: Float?,
    val location: LatLng?,
    val types: List<String>?
)

data class DisplayName(val text: String?)
data class OpeningHours(@SerializedName("openNow") val openNow: Boolean?)

// ── Implementation ──────────────────────────────────────────────────────────

class GooglePlacesApiImpl @Inject constructor(
    private val service: GooglePlacesRetrofitService,
    private val apiKey: String
) {
    suspend fun searchNearby(
        latitude: Double,
        longitude: Double,
        careType: CareType,
        radiusMeters: Int
    ): List<NearbyFacility> {
        val types = careTypeToPlacesTypes(careType)
        val request = PlacesNearbyRequest(
            includedTypes = types,
            locationRestriction = LocationRestriction(
                circle = Circle(
                    center = LatLng(latitude, longitude),
                    radius = radiusMeters.toDouble()
                )
            )
        )
        val response = service.searchNearby(apiKey, GooglePlacesRetrofitService.FIELD_MASK, request)
        return response.places?.map { it.toNearbyFacility(careType, latitude, longitude) }
            ?: emptyList()
    }

    private fun careTypeToPlacesTypes(careType: CareType): List<String> = when (careType) {
        CareType.EMERGENCY_ROOM -> listOf("hospital")
        CareType.URGENT_CARE -> listOf("hospital", "urgent_care_center")
        CareType.PRIMARY_CARE -> listOf("urgent_care_center", "medical_clinic")
        CareType.PHARMACY -> listOf("pharmacy")
        CareType.TELEHEALTH, CareType.HOME_CARE -> emptyList()
    }

    private fun PlaceResult.toNearbyFacility(careType: CareType, userLat: Double, userLng: Double): NearbyFacility {
        val distKm = location?.let { approximateDistanceKm(userLat, userLng, it.latitude, it.longitude) } ?: 0.0
        return NearbyFacility(
            name = displayName?.text ?: "Unknown",
            address = formattedAddress ?: "",
            phone = phone,
            type = careType,
            distanceKm = distKm,
            isOpen = openingHours?.openNow,
            rating = rating,
            mapsPlaceId = id ?: ""
        )
    }

    // Simple Euclidean approximation — sufficient for <50km facility search
    private fun approximateDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = (lat2 - lat1) * 111.0
        val dLng = (lng2 - lng1) * 111.0 * Math.cos(Math.toRadians(lat1))
        return sqrt(dLat * dLat + dLng * dLng)
    }
}
