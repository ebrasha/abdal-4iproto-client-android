/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : LatencyMeasurer.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:29:39
 * Description : Measures TCP connect latency to the active SSH server for dashboard chips and stats.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket

enum class LatencyQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNKNOWN
}

data class LatencyState(
    val latencyMs: Long? = null,
    val quality: LatencyQuality = LatencyQuality.UNKNOWN
)

/**
 * Periodically probes TCP connect time to the configured SSH endpoint.
 */
class LatencyMeasurer(
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(LatencyState())
    val state: StateFlow<LatencyState> = _state.asStateFlow()

    private var measureJob: Job? = null
    private var targetHost: String? = null
    private var targetPort: Int = 22
    private var measureIntervalMs = PingIntervalConfig.DEFAULT_SECONDS * 1_000L
    private var measurementEnabled = true

    fun setEnabled(enabled: Boolean) {
        if (measurementEnabled == enabled) {
            return
        }
        measurementEnabled = enabled
        if (!enabled) {
            stop()
            _state.value = LatencyState()
        } else if (targetHost != null) {
            restart()
        }
    }

    fun setMeasureIntervalSeconds(seconds: Int) {
        val clamped = PingIntervalConfig.coerce(seconds)
        val newIntervalMs = clamped * 1_000L
        if (newIntervalMs == measureIntervalMs) {
            return
        }
        measureIntervalMs = newIntervalMs
        if (targetHost != null) {
            restart()
        }
    }

    fun updateTarget(host: String?, port: Int) {
        targetHost = host?.trim()?.takeIf { it.isNotEmpty() }
        targetPort = port
        if (targetHost == null || !measurementEnabled) {
            _state.value = LatencyState()
            stop()
        } else {
            restart()
        }
    }

    fun restart() {
        stop()
        if (!measurementEnabled) {
            return
        }
        val host = targetHost ?: return
        measureJob = scope.launch {
            measureOnce(host, targetPort)
            while (isActive) {
                delay(measureIntervalMs)
                measureOnce(host, targetPort)
            }
        }
    }

    fun stop() {
        measureJob?.cancel()
        measureJob = null
    }

    private suspend fun measureOnce(host: String, port: Int) {
        val latency = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            measureTcpLatency(host, port)
        }
        _state.update {
            it.copy(
                latencyMs = latency,
                quality = classifyLatency(latency)
            )
        }
    }

    private fun measureTcpLatency(host: String, port: Int): Long? {
        return try {
            val start = System.nanoTime()
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            }
            ((System.nanoTime() - start) / 1_000_000L).coerceAtLeast(0L)
        } catch (_: Exception) {
            null
        }
    }

    private fun classifyLatency(ms: Long?): LatencyQuality {
        if (ms == null) {
            return LatencyQuality.UNKNOWN
        }
        return when {
            ms < 50 -> LatencyQuality.EXCELLENT
            ms < 100 -> LatencyQuality.GOOD
            ms < 200 -> LatencyQuality.FAIR
            else -> LatencyQuality.POOR
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000
    }
}
