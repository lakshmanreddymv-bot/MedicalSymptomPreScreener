package com.example.medicalsymptomprescreener.domain.monitor

import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {
    val isConnected: StateFlow<Boolean>
    fun isConnectedNow(): Boolean
}
