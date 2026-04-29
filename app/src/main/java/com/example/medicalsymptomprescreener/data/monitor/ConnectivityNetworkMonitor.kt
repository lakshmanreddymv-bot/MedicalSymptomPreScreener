package com.example.medicalsymptomprescreener.data.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import com.example.medicalsymptomprescreener.domain.monitor.NetworkMonitor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [NetworkMonitor] using [ConnectivityManager] callbacks.
 *
 * Uses `NET_CAPABILITY_VALIDATED` rather than `NET_CAPABILITY_INTERNET` to confirm
 * actual internet reachability — a device can be connected to Wi-Fi without internet access.
 * [callbackFlow] bridges the callback API into a coroutine-friendly [StateFlow].
 *
 * The flow is shared with [SharingStarted.WhileSubscribed] (5 s timeout) so the
 * [ConnectivityManager] callback is unregistered when no one is observing.
 *
 * S: Single Responsibility — observes and reports network connectivity state.
 * D: Dependency Inversion — implements [NetworkMonitor] interface.
 */
@Singleton
class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Hot [StateFlow] emitting `true` when the device has a validated internet connection.
     *
     * Registered with [ConnectivityManager.registerNetworkCallback] to receive live updates.
     * Emits the current state immediately on first collection.
     * Unregisters the callback when there are no active subscribers (5 s grace period).
     */
    override val isConnected: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            }
            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        // Emit current state immediately
        trySend(isConnectedNow())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.IO),
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = isConnectedNow()
    )

    /**
     * Returns the current connectivity state synchronously.
     *
     * Checks `NET_CAPABILITY_VALIDATED` on the active network. Returns `false` if
     * there is no active network or if the network lacks confirmed internet access.
     * Called by [TriageSymptomUseCase] before every Gemini API call.
     */
    override fun isConnectedNow(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
