package com.hiittimer.domain

import com.hiittimer.audio.AudioCue
import com.hiittimer.audio.AudioCuePlayer
import com.hiittimer.data.PreferencesStore
import com.hiittimer.haptics.HapticCue
import com.hiittimer.haptics.HapticFeedback
import com.hiittimer.speech.SpeechPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class EngineStatus { IDLE, RUNNING, PAUSED, FINISHED }

/**
 * Drives a workout: tracks elapsed wall-clock time, publishes state and
 * triggers sound/haptic/speech cues.
 *
 * Never counts ticks — measures elapsed time so a workout stays accurate even
 * when the ticker fires late. Mirrors iOS WorkoutEngine.
 */
class WorkoutEngine(
    val plan: WorkoutPlan,
    private val audio: AudioCuePlayer,
    private val haptics: HapticFeedback,
    private val preferences: PreferencesStore,
    private val speech: SpeechPlayer? = null,
    /** Time source in millis. Tests replace it with a controllable clock. */
    var clockMillis: () -> Long = { System.currentTimeMillis() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val _status = MutableStateFlow(EngineStatus.IDLE)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private val _state = MutableStateFlow(plan.state(at = 0.0))
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private var segmentStartMillis: Long = 0L
    private var accumulatedSecs: Double = 0.0
    private var ticker: Job? = null
    private var lastPhaseIndex = -1
    private var lastCueSecond = Int.MIN_VALUE

    /** Seconds of the workout completed so far. */
    val elapsedSecs: Double get() = when (_status.value) {
        EngineStatus.IDLE -> 0.0
        EngineStatus.FINISHED -> plan.totalDuration
        EngineStatus.PAUSED -> accumulatedSecs
        EngineStatus.RUNNING -> maxOf(accumulatedSecs + (clockMillis() - segmentStartMillis) / 1000.0, 0.0)
    }

    fun start() {
        if (_status.value != EngineStatus.IDLE) return
        accumulatedSecs = 0.0
        segmentStartMillis = clockMillis()
        _status.value = EngineStatus.RUNNING
        lastPhaseIndex = -1
        lastCueSecond = Int.MIN_VALUE
        audio.activate()
        scheduleTicker()
        tick()
    }

    fun pause() {
        if (_status.value != EngineStatus.RUNNING) return
        accumulatedSecs = elapsedSecs
        _status.value = EngineStatus.PAUSED
        cancelTicker()
        play(AudioCue.PAUSE); haptic(HapticCue.PAUSE)
    }

    fun resume() {
        if (_status.value != EngineStatus.PAUSED) return
        segmentStartMillis = clockMillis()
        _status.value = EngineStatus.RUNNING
        scheduleTicker()
        audio.activate()
        play(AudioCue.RESUME); haptic(HapticCue.RESUME)
        tick()
    }

    fun reset() {
        cancelTicker()
        _status.value = EngineStatus.IDLE
        accumulatedSecs = 0.0
        lastPhaseIndex = -1
        lastCueSecond = Int.MIN_VALUE
        _state.value = plan.state(at = 0.0)
        audio.deactivate()
        speech?.stop()
        haptic(HapticCue.RESET)
    }

    /** Re-reads the clock. Called when the app returns to the foreground. */
    fun sync() {
        if (_status.value != EngineStatus.RUNNING) return
        tick()
    }

    fun invalidate() = cancelTicker()

    private fun scheduleTicker() {
        cancelTicker()
        ticker = scope.launch {
            while (true) {
                delay(50)
                tick()
            }
        }
    }

    private fun cancelTicker() { ticker?.cancel(); ticker = null }

    fun tick() {
        val current = plan.state(at = elapsedSecs)
        _state.value = current
        if (current.isComplete) {
            if (_status.value == EngineStatus.RUNNING) finish()
            return
        }
        if (_status.value != EngineStatus.RUNNING) return
        if (current.phaseIndex != lastPhaseIndex) {
            lastPhaseIndex = current.phaseIndex
            lastCueSecond = Int.MIN_VALUE
            play(AudioCue.startOf(current.phase.phase))
            haptic(HapticCue.PHASE_CHANGE)
            announce(current)
            return
        }
        emitCountdownTick(current)
    }

    private fun emitCountdownTick(current: WorkoutState) {
        val seconds = current.secondsRemaining
        if (seconds == lastCueSecond) return
        lastCueSecond = seconds
        val prefs = preferences.preferences.value
        if (!prefs.soundEnabled || !prefs.countdownTicksEnabled) return
        if (seconds <= 0 || seconds > 3) return
        if (seconds >= Math.round(current.phase.duration).toInt()) return
        play(AudioCue.TICK); haptic(HapticCue.TICK)
    }

    private fun finish() {
        cancelTicker()
        _status.value = EngineStatus.FINISHED
        _state.value = plan.state(at = Double.POSITIVE_INFINITY)
        play(AudioCue.WORKOUT_COMPLETE)
        announce(_state.value)
        haptic(HapticCue.COMPLETE)
    }

    private fun announce(current: WorkoutState) {
        val prefs = preferences.preferences.value
        if (!prefs.announcementsEnabled) return
        speech?.speak(
            current.speechAnnouncement,
            prefs.speechVoiceID, prefs.speechLanguageCode, prefs.speechRate, prefs.speechVolume,
        )
    }

    private fun play(cue: AudioCue) {
        val prefs = preferences.preferences.value
        if (!prefs.soundEnabled) return
        audio.volume = prefs.volume.toFloat()
        audio.play(cue)
    }

    private fun haptic(cue: HapticCue) {
        if (!preferences.preferences.value.hapticsEnabled) return
        haptics.play(cue)
    }
}
