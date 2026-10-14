package com.hiittimer.domain

import kotlin.math.ceil
import kotlin.math.max

/** A snapshot of the workout at a single point in time. Times are seconds. */
data class WorkoutState(
    val phaseIndex: Int,
    val phase: PlannedPhase,
    val nextPhase: PlannedPhase?,
    val phaseElapsed: Double,
    val phaseRemaining: Double,
    val totalElapsed: Double,
    val totalDuration: Double,
    val isComplete: Boolean,
    val workout: Workout,
) {
    /** Seconds left in the current interval, rounded up (never 0 before it ends). */
    val secondsRemaining: Int get() {
        if (phaseRemaining <= 0) return 0
        return max(ceil(phaseRemaining - 1e-9).toInt(), 0)
    }

    val secondsElapsed: Int get() = phaseElapsed.toInt()

    /** Progress through the current interval, 0..1. */
    val phaseProgress: Double get() {
        if (phase.duration <= 0) return 1.0
        return (phaseElapsed / phase.duration).coerceIn(0.0, 1.0)
    }

    /** Progress through the whole workout, 0..1. */
    val totalProgress: Double get() {
        if (totalDuration <= 0) return 1.0
        return (totalElapsed / totalDuration).coerceIn(0.0, 1.0)
    }

    /** "Deadlifts. WORK. Round 2 of 8, set 1 of 1. 20 seconds remaining." */
    val accessibilityAnnouncement: String get() {
        if (isComplete) return "Workout complete."
        val sb = StringBuilder()
        phase.name?.let { sb.append("$it. ") }
        sb.append("${phase.phase.accessibilityDescription}.")
        if (phase.phase == WorkoutPhase.WORK) {
            sb.append(" Round ${phase.round} of ${workout.rounds}.")
            if (workout.sets > 1) sb.append(" Set ${phase.set} of ${workout.sets}.")
        }
        val s = secondsRemaining
        sb.append(" $s second${if (s == 1) "" else "s"} remaining.")
        return sb.toString()
    }

    /** Spoken when an interval begins. */
    val speechAnnouncement: String get() {
        if (isComplete) return "Workout complete."
        if (phase.phase == WorkoutPhase.WORK && phase.name != null) {
            return "Next exercise: ${phase.name}."
        }
        return "${phase.phase.accessibilityDescription}."
    }

    companion object {
        fun empty(totalDuration: Double, workout: Workout): WorkoutState {
            val placeholder = PlannedPhase(WorkoutPhase.WORK, 1, 1, 0.0, 0.0)
            return WorkoutState(0, placeholder, null, 0.0, 0.0, 0.0, totalDuration, false, workout)
        }
    }
}

/** Time formatting helpers. */
object TimeFormat {
    /** 20 -> "00:20", 3661 -> "01:01:01". */
    fun clock(intervalSecs: Double): String {
        val total = maxOf(Math.round(intervalSecs).toInt(), 0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    /** 265 -> "4:25", 3661 -> "1:01:01". */
    fun duration(intervalSecs: Long): String = duration(intervalSecs.toDouble())
    fun duration(intervalSecs: Double): String {
        val total = maxOf(Math.round(intervalSecs).toInt(), 0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
