/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : TunnelLogger.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 19:48:20
 * Description : Structured in-app log buffer with optional capture, coalesced UI updates, and Logcat mirroring.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel {
    INFO,
    WARN,
    ERROR;

    val wireCode: String
        get() = when (this) {
            INFO -> "I"
            WARN -> "W"
            ERROR -> "E"
        }
}

data class LogEntry(
    val id: Long,
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    val formattedLine: String
        get() = "$timestamp ${level.wireCode}/$tag: $message"
}

/**
 * Thread-safe, in-memory ring buffer of structured log entries.
 * The connection pipeline writes here so the user can inspect tunnel activity in-app.
 */
object TunnelLogger {

    const val PREFS_KEY_LOGGING_ENABLED = "app_logging_enabled"

    private const val MAX_LINES = 800
    private const val UI_EMIT_INTERVAL_MS = 300L
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val buffer = ArrayDeque<LogEntry>()
    private val lock = Any()
    private val enabled = AtomicBoolean(false)
    private val nextEntryId = AtomicLong(0L)
    private val uiEmitPending = AtomicBoolean(false)

    private val uiEmitJob = SupervisorJob()
    private val uiEmitScope = CoroutineScope(Dispatchers.Default + uiEmitJob)
    private val snapshotMutex = Mutex()
    private var cachedSnapshot: List<LogEntry> = emptyList()

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun setEnabled(value: Boolean) {
        enabled.set(value)
    }

    fun isEnabled(): Boolean = enabled.get()

    fun info(tag: String, message: String) {
        Log.i(tag, message)
        append(LogLevel.INFO, tag, message)
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        val fullMessage = message + (throwable?.let { " :: ${it.message}" } ?: "")
        append(LogLevel.WARN, tag, fullMessage)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val fullMessage = message + (
            throwable?.let { " :: ${it.javaClass.simpleName}: ${it.message}" } ?: ""
            )
        append(LogLevel.ERROR, tag, fullMessage)
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            cachedSnapshot = emptyList()
            _entries.value = emptyList()
        }
    }

    fun dump(): String = synchronized(lock) {
        buffer.joinToString(separator = "\n") { it.formattedLine }
    }

    fun dumpEntry(entry: LogEntry): String = entry.formattedLine

    private fun append(level: LogLevel, tag: String, message: String) {
        if (!enabled.get()) {
            return
        }
        synchronized(lock) {
            val entry = LogEntry(
                id = nextEntryId.incrementAndGet(),
                timestamp = timeFormat.format(Date()),
                level = level,
                tag = tag,
                message = message
            )
            buffer.addLast(entry)
            while (buffer.size > MAX_LINES) {
                buffer.removeFirst()
            }
        }
        scheduleUiEmit()
    }

    private fun scheduleUiEmit() {
        if (!uiEmitPending.compareAndSet(false, true)) {
            return
        }
        uiEmitScope.launch {
            delay(UI_EMIT_INTERVAL_MS)
            publishSnapshot()
            uiEmitPending.set(false)
        }
    }

    private suspend fun publishSnapshot() {
        val snapshot = synchronized(lock) { buffer.toList() }
        snapshotMutex.withLock {
            if (snapshot === cachedSnapshot || snapshot == cachedSnapshot) {
                return
            }
            cachedSnapshot = snapshot
            _entries.value = snapshot
        }
    }
}
