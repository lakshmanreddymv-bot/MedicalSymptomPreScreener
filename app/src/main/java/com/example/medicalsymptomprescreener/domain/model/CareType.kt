package com.example.medicalsymptomprescreener.domain.model

/**
 * Recommended care destination returned by the triage pipeline.
 *
 * [TELEHEALTH] and [HOME_CARE] are handled by [GuidanceScreen] — no Places API call is made.
 * All other types trigger a [FacilitiesScreen] search via Google Places API v1.
 *
 * S: Single Responsibility — classifies the recommended care destination only.
 */
enum class CareType {
    /** Hospital emergency room. Mapped to Places type: `hospital`. */
    EMERGENCY_ROOM,

    /** Urgent care center or hospital walk-in. Mapped to Places types: `hospital`, `urgent_care_center`. */
    URGENT_CARE,

    /** Primary care physician or medical clinic. Mapped to Places types: `urgent_care_center`, `medical_clinic`. */
    PRIMARY_CARE,

    /** Pharmacy for OTC remedies. Mapped to Places type: `pharmacy`. */
    PHARMACY,

    /** Virtual/video appointment. Skips Places API — routes to [GuidanceScreen]. */
    TELEHEALTH,

    /** Rest and home treatment. Skips Places API — routes to [GuidanceScreen]. */
    HOME_CARE
}
