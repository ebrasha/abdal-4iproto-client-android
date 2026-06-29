/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : HostValidator.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 19:48:20
 * Description : Validates server hostnames and IP addresses for server forms.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

import java.net.InetAddress

object HostValidator {

    /**
     * Returns true when [raw] is a valid IPv4/IPv6 literal or a legal hostname.
     */
    fun isValidHostOrIp(raw: String): Boolean {
        val value = raw.trim()
        if (value.isEmpty() || value.length > 253) {
            return false
        }
        if (isValidIpv4(value) || isValidIpv6(value)) {
            return true
        }
        return isValidHostname(value)
    }

    private fun isValidIpv4(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) {
            return false
        }
        return parts.all { part ->
            val octet = part.toIntOrNull() ?: return false
            octet in 0..255 && (part.length == 1 || !part.startsWith('0'))
        }
    }

    private fun isValidIpv6(value: String): Boolean {
        return try {
            val address = InetAddress.getByName(value)
            address.address.size == 16 && value.contains(':')
        } catch (_: Exception) {
            false
        }
    }

    private fun isValidHostname(value: String): Boolean {
        if (value.startsWith('.') || value.endsWith('.') || value.contains("..")) {
            return false
        }
        val labels = value.split('.')
        return labels.all { label ->
            label.isNotEmpty() &&
                label.length <= 63 &&
                label.all { it.isLetterOrDigit() || it == '-' } &&
                !label.startsWith('-') &&
                !label.endsWith('-')
        }
    }
}
