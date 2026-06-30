/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : DashboardRefreshConfig.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-30 19:24:21
 * Description : Shared preferences keys and bounds for dashboard stats and chart refresh intervals.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

object DashboardRefreshConfig {
    const val PREFS_KEY_STATS_MS = "dashboard_stats_interval_ms"
    const val PREFS_KEY_CHART_MS = "dashboard_chart_interval_ms"

    const val DEFAULT_STATS_MS = 1_000
    const val DEFAULT_CHART_MS = 2_000
    const val MIN_STATS_MS = 1_000
    const val MIN_CHART_MS = 2_000
    const val MAX_MS = 30_000
    const val STEP_MS = 1_000

    fun coerceStatsMs(millis: Int): Int =
        ((millis + STEP_MS / 2) / STEP_MS * STEP_MS).coerceIn(MIN_STATS_MS, MAX_MS)

    fun coerceChartMs(millis: Int, statsMs: Int = DEFAULT_STATS_MS): Int {
        val coerced = ((millis + STEP_MS / 2) / STEP_MS * STEP_MS).coerceIn(MIN_CHART_MS, MAX_MS)
        return coerced.coerceAtLeast(coerceStatsMs(statsMs))
    }
}
