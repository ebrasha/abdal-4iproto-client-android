/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : SSH Tunnel
 * File Name : TunnelLogger.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 15:59:00
 * Description : In-app, lifecycle-wide log buffer that mirrors events to Logcat and exposes them to the UI.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thread-safe, in-memory ring buffer of human readable log lines.
 * The connection pipeline writes here so the user can inspect everything that happens behind the scenes.
 */
object TunnelLogger {

    private const val MAX_LINES = 800
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val buffer = ArrayDeque<String>()
    private val lock = Any()

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    /**
     * Appends an informational line.
     */
    fun info(tag: String, message: String) {
        Log.i(tag, message)
        append("I", tag, message)
    }

    /**
     * Appends a warning line.
     */
    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        append("W", tag, message + (throwable?.let { " :: ${it.message}" } ?: ""))
    }

    /**
     * Appends an error line.
     */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        append("E", tag, message + (throwable?.let { " :: ${it.javaClass.simpleName}: ${it.message}" } ?: ""))
    }

    /**
     * Clears the whole buffer.
     */
    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _lines.value = emptyList()
        }
    }

    /**
     * Returns the buffer joined as a single block of text for sharing/copying.
     */
    fun dump(): String = synchronized(lock) { buffer.joinToString(separator = "\n") }

    private fun append(level: String, tag: String, message: String) {
        val line = "${timeFormat.format(Date())} $level/$tag: $message"
        synchronized(lock) {
            buffer.addLast(line)
            while (buffer.size > MAX_LINES) {
                buffer.removeFirst()
            }
            _lines.value = buffer.toList()
        }
    }
}
