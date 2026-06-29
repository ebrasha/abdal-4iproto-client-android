/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : PingIntervalConfig.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 06:45:00
 * Description : Shared preferences key and bounds for dashboard latency probe interval.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

object PingIntervalConfig {
    const val PREFS_KEY = "ping_interval_seconds"
    const val PREFS_KEY_ENABLED = "ping_measurement_enabled"
    const val DEFAULT_SECONDS = 20
    const val MIN_SECONDS = 5
    const val MAX_SECONDS = 120
    const val STEP_SECONDS = 5

    fun coerce(seconds: Int): Int =
        ((seconds + STEP_SECONDS / 2) / STEP_SECONDS * STEP_SECONDS)
            .coerceIn(MIN_SECONDS, MAX_SECONDS)
}
