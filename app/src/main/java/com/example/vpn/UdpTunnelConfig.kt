/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : SSH Tunnel
 * File Name : UdpTunnelConfig.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-07 07:21:20
 * Description : Immutable, validated configuration for the Abdal 4iProto UDP-over-SSH tunnel. Loads the
 *               user-customisable local settings from SharedPreferences (the same keys written by the
 *               UDP Tunnel settings screen) and clamps every value to its legal range. Protocol wire
 *               constants are kept here as fixed values and are intentionally not user-configurable.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.vpn

import android.content.Context

/**
 * Local, user-customisable settings for the UDP tunnel. None of these values affect the wire format;
 * they only control the local UDP socket and keepalive cadence.
 */
data class UdpTunnelConfig(
    val enabled: Boolean,
    val keepaliveIntervalSeconds: Int,
    val socketBufferBytes: Int,
    val readBufferSize: Int,
    val bindAddress: String,
    val localUdpPort: Int,
    val targetHost: String,
    val targetPort: Int
) {

    companion object {
        // SharedPreferences store shared with the UI layer (UdpViewModel).
        private const val PREFS_NAME = "abdal_vpn_prefs"

        private const val KEY_ENABLED = "udp_tunnel_enabled"
        private const val KEY_KEEPALIVE = "udp_keepalive_interval"
        private const val KEY_SOCKET_BUFFER_KB = "udp_socket_buffer"
        private const val KEY_READ_BUFFER = "udp_read_buffer_size"
        private const val KEY_BIND_ADDRESS = "udp_bind_address"
        private const val KEY_LOCAL_PORT = "udp_local_port"
        private const val KEY_TARGET_HOST = "udp_target_host"
        private const val KEY_TARGET_PORT = "udp_target_port"

        // Defaults mirror the UI defaults so a freshly installed app behaves predictably.
        private const val DEFAULT_KEEPALIVE_SECONDS = 30
        private const val DEFAULT_SOCKET_BUFFER_KB = 4096
        private const val DEFAULT_READ_BUFFER = 65535
        private const val DEFAULT_BIND_ADDRESS = "127.0.0.1"
        private const val DEFAULT_LOCAL_PORT = 0
        private const val DEFAULT_TARGET_HOST = "8.8.8.8"
        private const val DEFAULT_TARGET_PORT = 53

        // Hard limits derived from the 2-byte length framing and IP/port semantics.
        const val MAX_DATAGRAM_SIZE = 65535
        private const val MIN_PORT = 0
        private const val MAX_PORT = 65535

        /**
         * Reads and validates the UDP tunnel configuration from the shared preferences store.
         * Invalid or out-of-range values silently fall back to their safe defaults.
         */
        fun fromPreferences(context: Context): UdpTunnelConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val enabled = prefs.getBoolean(KEY_ENABLED, false)

            val keepalive = prefs.getString(KEY_KEEPALIVE, null)
                .toIntInRange(min = 1, max = Int.MAX_VALUE, fallback = DEFAULT_KEEPALIVE_SECONDS)

            // Buffer is provided in kilobytes by the UI; convert to bytes and guard against overflow.
            val socketBufferKb = prefs.getString(KEY_SOCKET_BUFFER_KB, null)
                .toIntInRange(min = 1, max = Int.MAX_VALUE / 1024, fallback = DEFAULT_SOCKET_BUFFER_KB)

            val readBuffer = prefs.getString(KEY_READ_BUFFER, null)
                .toIntInRange(min = 1, max = MAX_DATAGRAM_SIZE, fallback = DEFAULT_READ_BUFFER)

            val bindAddress = prefs.getString(KEY_BIND_ADDRESS, null)
                ?.trim()
                ?.takeIf { isValidIpv4(it) }
                ?: DEFAULT_BIND_ADDRESS

            val localPort = prefs.getString(KEY_LOCAL_PORT, null)
                .toIntInRange(min = MIN_PORT, max = MAX_PORT, fallback = DEFAULT_LOCAL_PORT)

            val targetHost = prefs.getString(KEY_TARGET_HOST, null)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_TARGET_HOST

            val targetPort = prefs.getString(KEY_TARGET_PORT, null)
                .toIntInRange(min = 1, max = MAX_PORT, fallback = DEFAULT_TARGET_PORT)

            return UdpTunnelConfig(
                enabled = enabled,
                keepaliveIntervalSeconds = keepalive,
                socketBufferBytes = socketBufferKb * 1024,
                readBufferSize = readBuffer,
                bindAddress = bindAddress,
                localUdpPort = localPort,
                targetHost = targetHost,
                targetPort = targetPort
            )
        }

        /** Parses a string to an Int, returning [fallback] when null, malformed, or out of range. */
        private fun String?.toIntInRange(min: Int, max: Int, fallback: Int): Int {
            val value = this?.trim()?.toIntOrNull() ?: return fallback
            return if (value in min..max) value else fallback
        }

        /** Lightweight IPv4 dotted-quad validation (no DNS resolution). */
        private fun isValidIpv4(value: String): Boolean {
            val parts = value.split(".")
            if (parts.size != 4) return false
            return parts.all { part ->
                val octet = part.toIntOrNull() ?: return false
                octet in 0..255 && (part.length == 1 || !part.startsWith("0"))
            }
        }
    }
}
