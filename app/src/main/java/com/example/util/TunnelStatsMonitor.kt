/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : TunnelStatsMonitor.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:29:39
 * Description : Polls native hev tunnel stats on IO and exposes throttled live metrics and chart series.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

import android.content.Context
import androidx.compose.runtime.Immutable
import com.example.vpn.HevSocksTunnel
import com.example.vpn.VpnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

@Immutable
data class TrafficLiveStats(
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val peakSpeedMbps: Double = 0.0,
    val dataUsedTodayBytes: Long = 0L
)

@Immutable
data class TrafficChartSeries(
    val downloadHistory: List<Float> = emptyList(),
    val uploadHistory: List<Float> = emptyList()
)

/**
 * Polls [HevSocksTunnel.getStats] on a background dispatcher while connected.
 * Scalar stats and chart history are emitted at independent configurable intervals.
 */
class TunnelStatsMonitor(
    context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val monitorJob = SupervisorJob()
    private val monitorScope = CoroutineScope(Dispatchers.IO + monitorJob)

    private val _liveStats = MutableStateFlow(TrafficLiveStats())
    val liveStats: StateFlow<TrafficLiveStats> = _liveStats.asStateFlow()

    private val _chartSeries = MutableStateFlow(TrafficChartSeries())
    val chartSeries: StateFlow<TrafficChartSeries> = _chartSeries.asStateFlow()

    private var pollJob: Job? = null
    private var sessionPeakMbps = 0.0
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var hasBaseline = false

    private var statsIntervalMs = DashboardRefreshConfig.DEFAULT_STATS_MS
    private var chartIntervalMs = DashboardRefreshConfig.DEFAULT_CHART_MS
    private var lastStatsEmitMs = 0L
    private var lastChartEmitMs = 0L

    private val downloadHistory = ArrayDeque<Float>(HISTORY_CAPACITY)
    private val uploadHistory = ArrayDeque<Float>(HISTORY_CAPACITY)
    private val cachedDownloadHistory = mutableListOf<Float>()
    private val cachedUploadHistory = mutableListOf<Float>()

    private val lastSlowOpLogMs = AtomicLong(0L)
    private var cachedTodayKey: String? = null
    private var cachedTodayKeyMs = 0L

    fun setStatsIntervalMs(millis: Int) {
        statsIntervalMs = DashboardRefreshConfig.coerceStatsMs(millis)
        if (chartIntervalMs < statsIntervalMs) {
            chartIntervalMs = DashboardRefreshConfig.coerceChartMs(chartIntervalMs, statsIntervalMs)
        }
    }

    fun setChartIntervalMs(millis: Int) {
        chartIntervalMs = DashboardRefreshConfig.coerceChartMs(millis, statsIntervalMs)
    }

    fun onVpnStateChanged(vpnState: VpnState) {
        when (vpnState) {
            VpnState.CONNECTED -> startPolling()
            else -> stopPolling(resetSession = vpnState == VpnState.DISCONNECTED || vpnState == VpnState.ERROR)
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        monitorJob.cancelChildren()
    }

    fun destroy() {
        stop()
        monitorJob.cancel()
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) {
            return
        }
        hasBaseline = false
        lastStatsEmitMs = 0L
        lastChartEmitMs = 0L
        pollJob = monitorScope.launch {
            while (isActive) {
                val started = System.nanoTime()
                sampleStats()
                logSlowSampleIfNeeded(started)
                delay(pollDelayMs())
            }
        }
    }

    private fun pollDelayMs(): Long =
        minOf(statsIntervalMs, chartIntervalMs).toLong()

    private fun stopPolling(resetSession: Boolean) {
        pollJob?.cancel()
        pollJob = null
        hasBaseline = false
        lastStatsEmitMs = 0L
        lastChartEmitMs = 0L
        if (resetSession) {
            sessionPeakMbps = 0.0
            lastRxBytes = 0L
            lastTxBytes = 0L
            downloadHistory.clear()
            uploadHistory.clear()
            cachedDownloadHistory.clear()
            cachedUploadHistory.clear()
            _liveStats.value = TrafficLiveStats()
            _chartSeries.value = TrafficChartSeries()
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

        val downloadMbps = BitrateFormatter.bytesPerSecondToMbps(deltaRx.toDouble())
        val uploadMbps = BitrateFormatter.bytesPerSecondToMbps(deltaTx.toDouble())
        val combinedPeak = maxOf(downloadMbps, uploadMbps)
        if (combinedPeak > sessionPeakMbps) {
            sessionPeakMbps = combinedPeak
        }

        val todayBytes = persistDailyUsage(deltaRx + deltaTx)
        val now = System.currentTimeMillis()

        if (now - lastStatsEmitMs >= statsIntervalMs) {
            lastStatsEmitMs = now
            _liveStats.value = TrafficLiveStats(
                downloadMbps = downloadMbps,
                uploadMbps = uploadMbps,
                peakSpeedMbps = sessionPeakMbps,
                dataUsedTodayBytes = todayBytes
            )
        }

        if (now - lastChartEmitMs >= chartIntervalMs) {
            lastChartEmitMs = now
            pushHistory(downloadHistory, downloadMbps.toFloat())
            pushHistory(uploadHistory, uploadMbps.toFloat())
            copyHistorySnapshot()
            _chartSeries.value = TrafficChartSeries(
                downloadHistory = cachedDownloadHistory.toList(),
                uploadHistory = cachedUploadHistory.toList()
            )
        }
    }

    private fun copyHistorySnapshot() {
        cachedDownloadHistory.clear()
        cachedDownloadHistory.addAll(downloadHistory)
        cachedUploadHistory.clear()
        cachedUploadHistory.addAll(uploadHistory)
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

    private fun todayKey(): String {
        val now = System.currentTimeMillis()
        if (cachedTodayKey != null && now - cachedTodayKeyMs < 60_000L) {
            return cachedTodayKey!!
        }
        val key = DATE_FORMAT.format(Date(now))
        cachedTodayKey = key
        cachedTodayKeyMs = now
        return key
    }

    private fun logSlowSampleIfNeeded(startedNanos: Long) {
        val elapsedMs = (System.nanoTime() - startedNanos) / 1_000_000L
        if (elapsedMs <= SLOW_SAMPLE_THRESHOLD_MS) {
            return
        }
        val now = System.currentTimeMillis()
        val last = lastSlowOpLogMs.get()
        if (now - last < SLOW_LOG_COOLDOWN_MS) {
            return
        }
        if (lastSlowOpLogMs.compareAndSet(last, now)) {
            TunnelLogger.warn(TAG, "sampleStats took ${elapsedMs}ms")
        }
    }

    init {
        val storedDate = prefs.getString(KEY_DATE, "") ?: ""
        val storedBytes = if (storedDate == todayKey()) {
            prefs.getLong(KEY_BYTES, 0L)
        } else {
            0L
        }
        _liveStats.value = TrafficLiveStats(dataUsedTodayBytes = storedBytes)
    }

    companion object {
        private const val TAG = "TunnelStatsMonitor"
        private const val PREFS_NAME = "tunnel_stats_prefs"
        private const val KEY_DATE = "data_used_date"
        private const val KEY_BYTES = "data_used_bytes"
        private const val HISTORY_CAPACITY = 60
        private const val SLOW_SAMPLE_THRESHOLD_MS = 50L
        private const val SLOW_LOG_COOLDOWN_MS = 60_000L
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
