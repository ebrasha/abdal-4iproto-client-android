/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : TunnelStatsMonitor.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:29:39
 * Description : Polls native hev tunnel stats and exposes rolling traffic metrics for the dashboard.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

import android.content.Context
import com.example.vpn.HevSocksTunnel
import com.example.vpn.VpnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrafficStatsState(
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val downloadHistory: List<Float> = emptyList(),
    val uploadHistory: List<Float> = emptyList(),
    val peakSpeedMbps: Double = 0.0,
    val dataUsedTodayBytes: Long = 0L
)

/**
 * Polls [HevSocksTunnel.getStats] while connected and maintains chart history plus daily totals.
 */
class TunnelStatsMonitor(
    private val scope: CoroutineScope,
    context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(TrafficStatsState())
    val state: StateFlow<TrafficStatsState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var sessionPeakMbps = 0.0
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var hasBaseline = false

    private val downloadHistory = ArrayDeque<Float>(HISTORY_CAPACITY)
    private val uploadHistory = ArrayDeque<Float>(HISTORY_CAPACITY)

    fun onVpnStateChanged(vpnState: VpnState) {
        when (vpnState) {
            VpnState.CONNECTED -> startPolling()
            else -> stopPolling(resetSession = vpnState == VpnState.DISCONNECTED || vpnState == VpnState.ERROR)
        }
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) {
            return
        }
        hasBaseline = false
        pollJob = scope.launch {
            while (isActive) {
                sampleStats()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPolling(resetSession: Boolean) {
        pollJob?.cancel()
        pollJob = null
        hasBaseline = false
        if (resetSession) {
            sessionPeakMbps = 0.0
            lastRxBytes = 0L
            lastTxBytes = 0L
            downloadHistory.clear()
            uploadHistory.clear()
            _state.update {
                it.copy(
                    downloadMbps = 0.0,
                    uploadMbps = 0.0,
                    downloadHistory = emptyList(),
                    uploadHistory = emptyList(),
                    peakSpeedMbps = 0.0
                )
            }
        }
    }

    private fun sampleStats() {
        val stats = HevSocksTunnel.getStats() ?: return
        if (stats.size < 4) {
            return
        }
        val txBytes = stats[1]
        val rxBytes = stats[3]

        if (!hasBaseline) {
            lastRxBytes = rxBytes
            lastTxBytes = txBytes
            hasBaseline = true
            return
        }

        val deltaRx = (rxBytes - lastRxBytes).coerceAtLeast(0L)
        val deltaTx = (txBytes - lastTxBytes).coerceAtLeast(0L)
        lastRxBytes = rxBytes
        lastTxBytes = txBytes

        val downloadBps = deltaRx.toDouble()
        val uploadBps = deltaTx.toDouble()
        val downloadMbps = BitrateFormatter.bytesPerSecondToMbps(downloadBps)
        val uploadMbps = BitrateFormatter.bytesPerSecondToMbps(uploadBps)
        val combinedPeak = maxOf(downloadMbps, uploadMbps)
        if (combinedPeak > sessionPeakMbps) {
            sessionPeakMbps = combinedPeak
        }

        val sessionDelta = deltaRx + deltaTx
        val todayBytes = persistDailyUsage(sessionDelta)

        pushHistory(downloadHistory, downloadMbps.toFloat())
        pushHistory(uploadHistory, uploadMbps.toFloat())

        _state.update {
            it.copy(
                downloadMbps = downloadMbps,
                uploadMbps = uploadMbps,
                downloadHistory = downloadHistory.toList(),
                uploadHistory = uploadHistory.toList(),
                peakSpeedMbps = sessionPeakMbps,
                dataUsedTodayBytes = todayBytes
            )
        }
    }

    private fun pushHistory(buffer: ArrayDeque<Float>, value: Float) {
        if (buffer.size >= HISTORY_CAPACITY) {
            buffer.removeFirst()
        }
        buffer.addLast(value)
    }

    private fun persistDailyUsage(deltaBytes: Long): Long {
        val todayKey = todayKey()
        val storedDate = prefs.getString(KEY_DATE, "") ?: ""
        val storedBytes = if (storedDate == todayKey) {
            prefs.getLong(KEY_BYTES, 0L)
        } else {
            0L
        }
        val updated = storedBytes + deltaBytes
        prefs.edit()
            .putString(KEY_DATE, todayKey)
            .putLong(KEY_BYTES, updated)
            .apply()
        return updated
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    init {
        val storedDate = prefs.getString(KEY_DATE, "") ?: ""
        val storedBytes = if (storedDate == todayKey()) {
            prefs.getLong(KEY_BYTES, 0L)
        } else {
            0L
        }
        _state.update { it.copy(dataUsedTodayBytes = storedBytes) }
    }

    companion object {
        private const val PREFS_NAME = "tunnel_stats_prefs"
        private const val KEY_DATE = "data_used_date"
        private const val KEY_BYTES = "data_used_bytes"
        private const val POLL_INTERVAL_MS = 1_000L
        private const val HISTORY_CAPACITY = 60
    }
}
