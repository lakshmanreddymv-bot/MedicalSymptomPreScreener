package com.example.medicalsymptomprescreener.data.api

import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.domain.model.NearbyFacility
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Retrofit interface for the Google Places API v1 nearby search endpoint.
 *
 * Uses the **new** `places.googleapis.com/v1/places:searchNearby` endpoint.
 * The legacy `maps.googleapis.com/maps/api/place/nearbysearch` endpoint is deprecated.
 *
 * API key and field mask are passed as headers (not query params) per the v1 spec.
 * [FIELD_MASK] controls which fields are returned — requesting only needed fields
 * reduces response size and billing cost.
 *
 * S: Single Responsibility — declares the Places API v1 nearby search HTTP endpoint.
 */
interface GooglePlacesRetrofitService {
    /**
     * Searches for nearby places matching the types in [request].
     *
     * @param apiKey Google Maps/Places API key from [BuildConfig.mapsapikey].
     * @param fieldMask Comma-separated field paths to include in the response.
     *   Always use [FIELD_MASK] — passing a different value can break [PlaceResult] mapping.
     * @param request Search parameters: place types, location circle, max results.
     */
    @POST("v1/places:searchNearby")
    suspend fun searchNearby(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body request: PlacesNearbyRequest
    ): PlacesNearbyResponse

    companion object {
        /** Standard field mask for all nearby searches. Requesting only these fields keeps billing low. */
        const val FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress," +
            "places.nationalPhoneNumber,places.currentOpeningHours," +
            "places.rating,places.location,places.types"
    }
}

// ── Request models ──────────────────────────────────────────────────────────

/**
 * Request body for the Places v1 `searchNearby` endpoint.
 *
 * [includedTypes] maps from [CareType] via [GooglePlacesApiImpl.careTypeToPlacesTypes].
 * Note: `doctor` type is excluded — it is sparsely indexed and returns near-empty results
 * in most areas. `urgent_care_center` is used for NON_URGENT instead.
 */
data class PlacesNearbyRequest(
    /** Place types to search for. Maximum 50 types per request. */
    @SerializedName("includedTypes") val includedTypes: List<String>,

    /** Geographic boundary circle centered on the user's location. */
    @SerializedName("locationRestriction") val locationRestriction: LocationRestriction,

    /** Maximum number of results to return (default 10, max 20). */
    @SerializedName("maxResultCount") val maxResultCount: Int = 10
)

/** Wraps a [Circle] to match the Places API locationRestriction structure. */
data class LocationRestriction(
    val circle: Circle
)

/** A geographic circle with a center [LatLng] and radius in metres. */
data class Circle(
    val center: LatLng,
    val radius: Double
)

/** Geographic coordinate pair used in both the request and in place results. */
data class LatLng(
    val latitude: Double,
    val longitude: Double
)

// ── Response models ─────────────────────────────────────────────────────────

/** Top-level response from the Places v1 `searchNearby` endpoint. */
data class PlacesNearbyResponse(
    /** List of matched places. Null if no results or on error. */
    val places: List<PlaceResult>?
)

/** A single place result from the Places API. All fields are nullable for safety. */
data class PlaceResult(
    /** Places API place ID. Used to build a Google Maps deep link. */
    val id: String?,

    /** Localized display name of the place. */
    @SerializedName("displayName") val displayName: DisplayName?,

    /** Full formatted street address. */
    @SerializedName("formattedAddress") val formattedAddress: String?,

    /** National phone number in local format if available. */
    @SerializedName("nationalPhoneNumber") val phone: String?,

    /** Current opening hours status. */
    @SerializedName("currentOpeningHours") val openingHours: OpeningHours?,

    /** Google rating 0–5. */
    val rating: Float?,

    /** Geographic coordinates of the place. Used for distance calculation. */
    val location: LatLng?,

    /** Place type tags (e.g. ["hospital", "health"]). */
    val types: List<String>?
)

/** Localized display name returned by the Places API. */
data class DisplayName(val text: String?)

/** Current opening hours state. */
data class OpeningHours(@SerializedName("openNow") val openNow: Boolean?)

// ── Implementation ──────────────────────────────────────────────────────────

/**
 * Searches for nearby medical facilities using the Google Places API v1.
 *
 * Maps [CareType] to Places API type strings. Chosen types avoid `doctor` — that type
 * is sparsely indexed in the Places API and returns near-empty results in most areas.
 * URGENT maps to `["hospital", "urgent_care_center"]` for maximum coverage.
 *
 * Distance is approximated with a flat-Earth Euclidean formula. Accurate enough
 * for facility searches within 50 km; avoids the Haversine formula overhead.
 *
 * S: Single Responsibility — wraps the Places API nearby search and result mapping.
 */
class GooglePlacesApiImpl @Inject constructor(
    private val service: GooglePlacesRetrofitService,

    /** Google Maps/Places API key from [BuildConfig.mapsapikey] (secrets plugin naming). */
    private val apiKey: String
) {
    /**
     * Searches for [NearbyFacility] matching [careType] within [radiusMeters] of the given coordinates.
     *
     * @param latitude User's current latitude.
     * @param longitude User's current longitude.
     * @param careType Determines which Places API types to include in the search.
     * @param radiusMeters Search radius in metres (default 10 000 m = 10 km).
     * @return List of mapped [NearbyFacility] results. Empty list if none found.
     */
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

    /**
     * Maps a [CareType] to the Places API `includedTypes` list.
     *
     * URGENT uses both `hospital` and `urgent_care_center` for maximum rural coverage.
     * `doctor` is excluded — it is sparsely indexed and returns near-empty results.
     * TELEHEALTH and HOME_CARE return empty (caller should use [FindNearbyFacilitiesUseCase.shouldSkipFacilitySearch]).
     */
    private fun careTypeToPlacesTypes(careType: CareType): List<String> = when (careType) {
        CareType.EMERGENCY_ROOM -> listOf("hospital")
        CareType.URGENT_CARE -> listOf("hospital", "urgent_care_center")
        CareType.PRIMARY_CARE -> listOf("urgent_care_center", "medical_clinic")
        CareType.PHARMACY -> listOf("pharmacy")
        CareType.TELEHEALTH, CareType.HOME_CARE -> emptyList()
    }

    /** Maps a [PlaceResult] to a [NearbyFacility] domain model. */
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

    /**
     * Approximates the distance between two lat/lng points in kilometres.
     * Uses a flat-Earth Euclidean formula — accurate within ~1% for distances under 50 km.
     * Sufficient for facility search; avoids the computational overhead of Haversine.
     */
    private fun approximateDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = (lat2 - lat1) * 111.0
        val dLng = (lng2 - lng1) * 111.0 * Math.cos(Math.toRadians(lat1))
        return sqrt(dLat * dLat + dLng * dLng)
    }
}
