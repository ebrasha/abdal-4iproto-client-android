/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : BitrateFormatter.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:29:39
 * Description : Formats byte rates and data sizes for dashboard charts and stats labels.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

import java.util.Locale
import kotlin.math.abs

object BitrateFormatter {

    /** Formats bits per second as B/s, kbps, Mbps, or Gbps. */
    fun formatSpeed(bps: Double): String {
        val value = abs(bps)
        return when {
            value >= 1_000_000_000 -> String.format(Locale.US, "%.1f Gbps", value / 1_000_000_000)
            value >= 1_000_000 -> String.format(Locale.US, "%.1f Mbps", value / 1_000_000)
            value >= 1_000 -> String.format(Locale.US, "%.0f kbps", value / 1_000)
            else -> String.format(Locale.US, "%.0f bps", value)
        }
    }

    /** Formats bytes per second for chart axis labels. */
    fun formatBytesPerSecond(bytesPerSecond: Double): String {
        val value = abs(bytesPerSecond)
        return when {
            value >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB/s", value / 1_000_000_000)
            value >= 1_000_000 -> String.format(Locale.US, "%.1f MB/s", value / 1_000_000)
            value >= 1_000 -> String.format(Locale.US, "%.0f KB/s", value / 1_000)
            else -> String.format(Locale.US, "%.0f B/s", value)
        }
    }

    /** Formats a cumulative byte count for the data-used stat. */
    fun formatDataSize(bytes: Long): String {
        val value = abs(bytes.toDouble())
        return when {
            value >= 1_000_000_000 -> String.format(Locale.US, "%.2f GB", value / 1_000_000_000)
            value >= 1_000_000 -> String.format(Locale.US, "%.1f MB", value / 1_000_000)
            value >= 1_000 -> String.format(Locale.US, "%.0f KB", value / 1_000)
            else -> String.format(Locale.US, "%.0f B", value)
        }
    }

    /** Converts bytes per second to megabits per second for peak-speed display. */
    fun bytesPerSecondToMbps(bytesPerSecond: Double): Double =
        (bytesPerSecond * 8.0) / 1_000_000.0
}
