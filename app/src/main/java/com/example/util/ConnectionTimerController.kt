/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : ConnectionTimerController.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:29:39
 * Description : Tracks VPN connection uptime with pause, resume, and reset rules for the dashboard timer.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

import android.os.SystemClock
import com.example.vpn.VpnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ConnectionTimerState(
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0
)

/**
 * Drives the dashboard connection timer according to VPN lifecycle events.
 */
class ConnectionTimerController {

    private val _state = MutableStateFlow(ConnectionTimerState())
    val state: StateFlow<ConnectionTimerState> = _state.asStateFlow()

    private var accumulatedMs = 0L
    private var lastTickRealtime = 0L
    private var isRunning = false
    private var resetOnNextConnected = false
    private var userInitiatedDisconnect = false
    private var wasEverConnected = false

    /** Call when the user presses Connect (not on auto-reconnect). */
    fun onUserConnectRequested() {
        userInitiatedDisconnect = false
        resetOnNextConnected = true
    }

    /** Call when the user presses Disconnect. */
    fun onUserDisconnectRequested() {
        userInitiatedDisconnect = true
        pause()
    }

    /** Reacts to global VPN state changes from [com.example.vpn.VpnStateNotifier]. */
    fun onVpnStateChanged(vpnState: VpnState) {
        when (vpnState) {
            VpnState.CONNECTED -> onConnected()
            VpnState.CONNECTING -> {
                if (isRunning && !userInitiatedDisconnect) {
                    pause()
                }
            }
            VpnState.DISCONNECTED, VpnState.ERROR -> {
                if (isRunning && !userInitiatedDisconnect) {
                    pause()
                }
            }
        }
    }

    /** Advances the timer by one second while the tunnel is connected. */
    fun tickSecond() {
        if (!isRunning) {
            return
        }
        val now = SystemClock.elapsedRealtime()
        accumulatedMs += (now - lastTickRealtime).coerceAtLeast(0L)
        lastTickRealtime = now
        publishElapsed()
    }

    private fun onConnected() {
        if (resetOnNextConnected || !wasEverConnected) {
            accumulatedMs = 0L
            resetOnNextConnected = false
        }
        wasEverConnected = true
        start()
        publishElapsed()
    }

    private fun start() {
        isRunning = true
        lastTickRealtime = SystemClock.elapsedRealtime()
    }

    private fun pause() {
        if (isRunning) {
            accumulatedMs += (SystemClock.elapsedRealtime() - lastTickRealtime).coerceAtLeast(0L)
            isRunning = false
            publishElapsed()
        }
    }

    private fun publishElapsed() {
        val totalSeconds = (accumulatedMs / 1_000L).coerceAtLeast(0L)
        val hours = (totalSeconds / 3_600L).toInt()
        val minutes = ((totalSeconds % 3_600L) / 60L).toInt()
        val seconds = (totalSeconds % 60L).toInt()
        _state.update {
            it.copy(
                hours = hours,
                minutes = minutes,
                seconds = seconds
            )
        }
    }
}
