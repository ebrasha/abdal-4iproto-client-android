/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : SSH Tunnel
 * File Name : FakeDnsResolver.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 23:56:47
 * Description : Fake-IP / FakeDNS engine. Synthesizes local DNS answers that map each requested domain to a
 *               reserved fake IPv4 address, keeping a fake-IP <-> domain mapping so that later TCP connections
 *               to that fake IP can be tunneled to the SSH server by domain name (remote DNS resolution).
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.vpn

import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Implements the fake-IP technique used to achieve server-side (remote) DNS resolution over a TUN interface.
 *
 * When enabled, DNS queries are answered locally with a synthetic IP taken from the reserved 198.18.0.0/15 range
 * (RFC 2544 benchmarking range, not routable on the public internet). The domain behind each fake IP is remembered,
 * so when an app later opens a TCP connection to that fake IP, the proxy can substitute the real domain name and let
 * the SSH server perform the actual resolution. As a result no real DNS query ever leaves the device.
 */
class FakeDnsResolver {

    private val domainToIp = ConcurrentHashMap<String, String>()
    private val ipToDomain = ConcurrentHashMap<String, String>()
    private val counter = AtomicInteger(INITIAL_OFFSET)

    /**
     * Returns true when the given IPv4 string belongs to the reserved fake-IP range.
     */
    fun isFakeIp(ip: String): Boolean {
        val value = ipToInt(ip) ?: return false
        return (value and FAKE_IP_MASK) == FAKE_IP_BASE
    }

    /**
     * Returns the domain previously associated with a fake IP, or null when no mapping exists.
     */
    fun lookup(ip: String): String? = ipToDomain[ip]

    /**
     * Builds a synthetic DNS response for the given query.
     *
     * For A queries a fake IP is allocated (or reused) and returned as the answer. For AAAA/HTTPS and other record
     * types a NODATA response is produced so clients fall back to the A record. Returns null when the query cannot be
     * parsed, allowing the caller to fall back to the regular DNS-over-TCP path.
     */
    fun synthesize(query: ByteArray): ByteArray? {
        if (query.size < HEADER_SIZE) return null
        // Only handle standard queries (QR bit clear) carrying exactly one question.
        if ((query[2].toInt() and 0x80) != 0) return null
        val qdCount = ((query[4].toInt() and 0xFF) shl 8) or (query[5].toInt() and 0xFF)
        if (qdCount != 1) return null

        var pos = HEADER_SIZE
        val domain = StringBuilder()
        while (true) {
            if (pos >= query.size) return null
            val len = query[pos].toInt() and 0xFF
            pos++
            if (len == 0) break
            // Label compression is not expected inside a query question.
            if ((len and 0xC0) != 0) return null
            if (pos + len > query.size) return null
            if (domain.isNotEmpty()) domain.append('.')
            domain.append(String(query, pos, len, Charsets.US_ASCII))
            pos += len
        }
        if (pos + 4 > query.size) return null
        val qType = ((query[pos].toInt() and 0xFF) shl 8) or (query[pos + 1].toInt() and 0xFF)
        val questionEnd = pos + 4

        val isA = qType == TYPE_A
        val out = ByteArrayOutputStream()

        // Header: echo ID, set response + recursion-desired + recursion-available flags.
        out.write(query[0].toInt() and 0xFF)
        out.write(query[1].toInt() and 0xFF)
        out.write(0x81)
        out.write(0x80)
        out.write(0x00); out.write(0x01)              // QDCOUNT
        if (isA) { out.write(0x00); out.write(0x01) } // ANCOUNT
        else { out.write(0x00); out.write(0x00) }
        out.write(0x00); out.write(0x00)              // NSCOUNT
        out.write(0x00); out.write(0x00)              // ARCOUNT

        // Echo the original question section verbatim.
        out.write(query, HEADER_SIZE, questionEnd - HEADER_SIZE)

        if (isA) {
            val ip = allocate(domain.toString())
            val ipBytes = ipToBytes(ip) ?: return null
            out.write(0xC0); out.write(0x0C)          // NAME pointer to the question
            out.write(0x00); out.write(TYPE_A)        // TYPE A
            out.write(0x00); out.write(0x01)          // CLASS IN
            out.write(0x00); out.write(0x00); out.write(0x00); out.write(ANSWER_TTL_SECONDS) // TTL
            out.write(0x00); out.write(0x04)          // RDLENGTH
            out.write(ipBytes)
        }
        return out.toByteArray()
    }

    /**
     * Allocates (or reuses) a fake IP for the given domain, keeping both mapping directions consistent.
     */
    private fun allocate(domain: String): String {
        domainToIp[domain]?.let { return it }
        synchronized(this) {
            domainToIp[domain]?.let { return it }
            val offset = nextOffset()
            val ip = intToIp(FAKE_IP_BASE + offset)
            domainToIp[domain] = ip
            ipToDomain[ip] = domain
            return ip
        }
    }

    private fun nextOffset(): Int {
        // Wrap around inside the usable part of the /15 range, skipping the first/last few addresses.
        val raw = counter.getAndIncrement()
        return INITIAL_OFFSET + (raw % USABLE_ADDRESSES)
    }

    private fun ipToBytes(ip: String): ByteArray? {
        val parts = ip.split('.')
        if (parts.size != 4) return null
        return ByteArray(4) { i -> (parts[i].toIntOrNull() ?: return null).toByte() }
    }

    private fun ipToInt(ip: String): Int? {
        val parts = ip.split('.')
        if (parts.size != 4) return null
        var value = 0
        for (part in parts) {
            val octet = part.toIntOrNull() ?: return null
            if (octet !in 0..255) return null
            value = (value shl 8) or octet
        }
        return value
    }

    private fun intToIp(value: Int): String {
        return "${(value ushr 24) and 0xFF}.${(value ushr 16) and 0xFF}." +
            "${(value ushr 8) and 0xFF}.${value and 0xFF}"
    }

    companion object {
        private const val HEADER_SIZE = 12
        private const val TYPE_A = 0x01
        private const val ANSWER_TTL_SECONDS = 10

        // 198.18.0.0/15 reserved benchmarking range used as the fake-IP pool.
        private const val FAKE_IP_BASE = (198 shl 24) or (18 shl 16)
        private const val FAKE_IP_MASK = -0x20000 // 255.254.0.0
        private const val INITIAL_OFFSET = 16
        // Leave a small margin at both ends of the /15 (131072 addresses) range.
        private const val USABLE_ADDRESSES = (1 shl 17) - 32
    }
}
