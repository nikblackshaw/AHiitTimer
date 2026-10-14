package com.hiittimer.haptics

/** Every distinct haptic the app can make. Mirrors iOS HapticCue. */
enum class HapticCue { START, PHASE_CHANGE, TICK, PAUSE, RESUME, RESET, COMPLETE }

interface HapticFeedback {
    fun play(cue: HapticCue)
}
