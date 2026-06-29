/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : PortParser.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:25:30
 * Description : Parses comma-separated SSH port lists and selects fixed or random ports for connect.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

import kotlin.random.Random

object PortParser {

    /** Parses a comma-separated port string into validated port numbers (1–65535). */
    fun parsePorts(raw: String): List<Int> {
        if (raw.isBlank()) {
            return listOf(22)
        }
        val ports = raw.split(',')
            .mapNotNull { token ->
                val value = token.trim().toIntOrNull()
                if (value != null && value in 1..65535) value else null
            }
        return ports.ifEmpty { listOf(22) }
    }

    /** Returns the first port in the list (fixed mode). */
    fun pickFixed(raw: String): Int = parsePorts(raw).first()

    /** Returns a random port from the list (random mode). */
    fun pickRandom(raw: String): Int {
        val ports = parsePorts(raw)
        return ports[Random.nextInt(ports.size)]
    }

    /** Validates that every comma-separated segment is a legal port. */
    fun isValidPortList(raw: String): Boolean {
        if (raw.isBlank()) {
            return false
        }
        val tokens = raw.split(',')
        if (tokens.isEmpty()) {
            return false
        }
        return tokens.all { token ->
            val value = token.trim().toIntOrNull()
            value != null && value in 1..65535
        }
    }
}
