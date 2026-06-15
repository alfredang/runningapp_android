package com.tertiaryinfotech.runtrackgps.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tracks elapsed running time. Truth comes from wall-clock time (so background
 * suspension or dropped ticks never lose time); a 1 Hz ticker only nudges the UI
 * to re-read [elapsed]. Mirrors the iOS RunTimerManager.
 */
class RunTimerManager(private val scope: CoroutineScope) {

    private val _elapsed = MutableStateFlow(0.0)        // seconds
    val elapsed: StateFlow<Double> = _elapsed.asStateFlow()

    var isRunning = false
        private set

    private var accumulated = 0.0                       // seconds
    private var segmentStartMs: Long? = null
    private var ticker: Job? = null

    private fun now() = System.currentTimeMillis()

    /** Elapsed = accumulated segments + the live segment. */
    private fun computeElapsed(): Double {
        val start = segmentStartMs ?: return accumulated
        return accumulated + (now() - start) / 1000.0
    }

    fun start() {
        reset()
        segmentStartMs = now()
        isRunning = true
        startTicker()
    }

    fun pause() {
        if (!isRunning) return
        accumulated = computeElapsed()
        segmentStartMs = null
        isRunning = false
        _elapsed.value = accumulated
        stopTicker()
    }

    fun resume() {
        if (isRunning) return
        segmentStartMs = now()
        isRunning = true
        startTicker()
    }

    fun stop() {
        accumulated = computeElapsed()
        segmentStartMs = null
        isRunning = false
        _elapsed.value = accumulated
        stopTicker()
    }

    fun reset() {
        accumulated = 0.0
        segmentStartMs = null
        isRunning = false
        _elapsed.value = 0.0
        stopTicker()
    }

    private fun startTicker() {
        stopTicker()
        ticker = scope.launch(Dispatchers.Default) {
            while (isActive) {
                _elapsed.value = computeElapsed()
                delay(1000)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }
}
