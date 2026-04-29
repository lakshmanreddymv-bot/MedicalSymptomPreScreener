package com.example.medicalsymptomprescreener.domain.monitor

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain-layer contract for observing network connectivity.
 *
 * Implemented by [ConnectivityNetworkMonitor] using [android.net.ConnectivityManager].
 * Used by [TriageSymptomUseCase] to gate the Gemini API call — if offline, returns
 * a safe URGENT fallback without ever touching the network.
 *
 * Keeping this as an interface in the domain layer allows unit tests to inject
 * a fake implementation without any Android framework dependencies.
 *
 * S: Single Responsibility — exposes network connectivity state only.
 * D: Dependency Inversion — domain depends on this abstraction, not on ConnectivityManager directly.
 */
interface NetworkMonitor {
    /**
     * A hot [StateFlow] that emits `true` when the device has a validated internet connection
     * and `false` when connectivity is lost. Uses [NET_CAPABILITY_VALIDATED] to confirm
     * real internet access, not just local Wi-Fi association.
     */
    val isConnected: StateFlow<Boolean>

    /**
     * Synchronous point-in-time connectivity check.
     * Returns `true` if the active network has [NET_CAPABILITY_VALIDATED].
     * Called by [TriageSymptomUseCase] before every Gemini API call.
     */
    fun isConnectedNow(): Boolean
}
