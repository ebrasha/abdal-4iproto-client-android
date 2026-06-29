/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : SoundManager.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-04 06:24:00
 * Description : Lightweight, process-wide sound effect player for tunnel lifecycle events.
 *               Uses a single SoundPool with samples loaded once via the application context to
 *               keep latency and memory footprint minimal and to avoid context leaks.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Plays short tunnel related sound effects.
 *
 * Design notes (performance / memory safety):
 *  - A single [SoundPool] is created once and reused for the whole process lifetime.
 *  - Samples are decoded and cached a single time on [init]; [play] is non-blocking.
 *  - Only the application context is retained, so no Activity/Service context can leak.
 */
object SoundManager {

    private var soundPool: SoundPool? = null

    private var startSoundId = 0
    private var connectSoundId = 0
    private var disconnectSoundId = 0
    private var errorSoundId = 0
    private var switchSoundId = 0

    // Ensures a burst of repeated failures triggers the error sound only once, until either the
    // next successful connection or a new user-initiated connect attempt re-arms it.
    private val errorSoundArmed = AtomicBoolean(true)

    /**
     * Loads all sound samples once. Safe to call multiple times and from multiple components
     * (service and view model); subsequent calls are no-ops.
     */
    @Synchronized
    fun init(context: Context) {
        if (soundPool != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        val appContext = context.applicationContext
        startSoundId = pool.load(appContext, R.raw.start, 1)
        connectSoundId = pool.load(appContext, R.raw.connect, 1)
        disconnectSoundId = pool.load(appContext, R.raw.disconnet, 1)
        errorSoundId = pool.load(appContext, R.raw.error, 1)
        switchSoundId = pool.load(appContext, R.raw.abdal_switch, 1)

        soundPool = pool
    }

    /** Plays the "start" cue and re-arms the error sound for a fresh connection cycle. */
    fun playStart() {
        errorSoundArmed.set(true)
        play(startSoundId)
    }

    /** Plays the "connected" cue (manual or auto-reconnect) and re-arms the error sound. */
    fun playConnect() {
        errorSoundArmed.set(true)
        play(connectSoundId)
    }

    /** Plays the "disconnected" cue for a user-initiated disconnect. */
    fun playDisconnect() {
        play(disconnectSoundId)
    }

    /** Plays the toggle cue used when a feature switch is turned on or off. */
    fun playSwitch() {
        play(switchSoundId)
    }

    /**
     * Plays the "error" cue, but only for the first failure in the current cycle. Subsequent
     * failures stay silent until a successful connection or a new connect attempt re-arms it.
     */
    fun playError() {
        if (errorSoundArmed.compareAndSet(true, false)) {
            play(errorSoundId)
        }
    }

    private fun play(soundId: Int) {
        if (soundId == 0) return
        soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    /** Releases the underlying [SoundPool]. Call only when sound playback is no longer needed. */
    @Synchronized
    fun release() {
        soundPool?.release()
        soundPool = null
        errorSoundArmed.set(true)
    }
}
