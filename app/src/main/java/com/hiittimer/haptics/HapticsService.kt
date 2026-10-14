package com.hiittimer.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Vibrator-backed haptics. All calls are no-ops when hardware is absent. */
class HapticsService(context: Context) : HapticFeedback {
    private val appContext = context.applicationContext

    private fun vibrator(): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) { null }

    private fun buzz(millis: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        try {
            val v = vibrator() ?: return
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(millis, amplitude))
            } else {
                @Suppress("DEPRECATION") v.vibrate(millis)
            }
        } catch (_: Exception) { }
    }

    override fun play(cue: HapticCue) {
        when (cue) {
            HapticCue.START, HapticCue.COMPLETE -> buzz(220)
            HapticCue.PHASE_CHANGE -> buzz(120, 255)
            HapticCue.TICK -> buzz(30, 120)
            HapticCue.PAUSE -> buzz(160)
            HapticCue.RESUME, HapticCue.RESET -> buzz(80)
        }
    }

    fun prepare() { /* no-op on Android */ }
}
