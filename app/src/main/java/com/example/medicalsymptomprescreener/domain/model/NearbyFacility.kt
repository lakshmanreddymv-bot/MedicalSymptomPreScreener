package com.example.medicalsymptomprescreener.domain.model

/**
 * A medical facility returned by the Google Places API v1 nearby search.
 *
 * Distance is approximated using a Euclidean formula (sufficient for <50 km searches).
 * Shown in [FacilitiesScreen] via [FacilityCard]. Tapping the directions icon
 * opens Google Maps with [mapsPlaceId] as the destination.
 *
 * S: Single Responsibility — models one nearby facility result from Places API.
 */
data class NearbyFacility(
    /** Display name of the facility (from Places `displayName.text`). */
    val name: String,

    /** Formatted street address (from Places `formattedAddress`). */
    val address: String,

    /** National phone number if available (from Places `nationalPhoneNumber`). */
    val phone: String?,

    /** Care type used to classify the facility and match it to the triage result. */
    val type: CareType,

    /** Approximate distance from the user's location in kilometres. */
    val distanceKm: Double,

    /** Whether the facility is currently open. Null if opening hours are not available. */
    val isOpen: Boolean?,

    /** Google rating (0–5). Null if no ratings exist. */
    val rating: Float?,

    /** Places API place ID. Used to build the Google Maps deep link. */
    val mapsPlaceId: String
)
