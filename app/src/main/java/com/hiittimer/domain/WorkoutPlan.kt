package com.hiittimer.domain

import kotlinx.serialization.Serializable

/** One interval inside a workout, with its absolute position on the timeline. */
@Serializable
data class PlannedPhase(
    val phase: WorkoutPhase,
    /** 1-based set number, 0 for warm-up and cool-down. */
    val set: Int,
    /** 1-based round number, 0 for warm-up and cool-down. */
    val round: Int,
    /** Seconds. */
    val duration: Double,
    /** Seconds from the start of the workout. */
    val start: Double,
    /** Resolved exercise (work) name, when the workout defines one. */
    val name: String? = null,
) {
    val end: Double get() = start + duration

    /** Short description used by announcements and the "next up" label. */
    val displayName: String get() = name ?: when (phase) {
        WorkoutPhase.WORK -> "Work"
        WorkoutPhase.REST -> "Rest"
        WorkoutPhase.WARM_UP -> "Warm up"
        WorkoutPhase.COOL_DOWN -> "Cool down"
    }
}

/**
 * The flattened timeline of a workout.
 *
 * Built once, then queried with elapsed time — the engine only tracks elapsed
 * time, never the state machine itself.
 */
data class WorkoutPlan(val workout: Workout, val phases: List<PlannedPhase>, val totalDuration: Double) {
    constructor(workout: Workout) : this(
        workout.validated(),
        buildPhases(workout.validated()),
        buildPhases(workout.validated()).fold(0.0) { acc, p -> p.end.coerceAtLeast(acc) },
    )

    /** State of the workout at a given elapsed time (seconds). Values are clamped. */
    fun state(at: Double): WorkoutState {
        val first = phases.firstOrNull()
            ?: return WorkoutState.empty(totalDuration, workout)
        if (at >= totalDuration) {
            val last = phases.last()
            return WorkoutState(
                phaseIndex = phases.size - 1,
                phase = last,
                nextPhase = null,
                phaseElapsed = last.duration,
                phaseRemaining = 0.0,
                totalElapsed = totalDuration,
                totalDuration = totalDuration,
                isComplete = true,
                workout = workout,
            )
        }
        val clamped = maxOf(at, 0.0)
        val index = phases.indexOfFirst { clamped < it.end }
        if (index < 0) {
            return WorkoutState(
                phaseIndex = phases.size - 1, phase = first, nextPhase = null,
                phaseElapsed = 0.0, phaseRemaining = 0.0,
                totalElapsed = 0.0, totalDuration = totalDuration,
                isComplete = false, workout = workout,
            )
        }
        val phase = phases[index]
        return WorkoutState(
            phaseIndex = index,
            phase = phase,
            nextPhase = if (index + 1 < phases.size) phases[index + 1] else null,
            phaseElapsed = maxOf(clamped - phase.start, 0.0),
            phaseRemaining = maxOf(phase.end - clamped, 0.0),
            totalElapsed = minOf(clamped, totalDuration),
            totalDuration = totalDuration,
            isComplete = false,
            workout = workout,
        )
    }

    companion object {
        private fun buildPhases(config: Workout): List<PlannedPhase> {
            val built = mutableListOf<PlannedPhase>()
            var cursor = 0.0
            fun append(phase: WorkoutPhase, set: Int, round: Int, durationSecs: Int, name: String? = null) {
                val d = durationSecs.toDouble()
                if (d <= 0) return
                built.add(PlannedPhase(phase, set, round, d, cursor, name))
                cursor += d
            }
            append(WorkoutPhase.WARM_UP, 0, 0, config.warmUpSeconds)
            for (set in 1..config.sets) {
                for (round in 1..config.rounds) {
                    append(WorkoutPhase.WORK, set, round, config.workSeconds, config.exerciseName(set, round))
                    // Every work interval is followed by a rest, including the last one.
                    append(WorkoutPhase.REST, set, round, config.restSeconds)
                }
            }
            append(WorkoutPhase.COOL_DOWN, 0, 0, config.coolDownSeconds)
            return built
        }
    }
}
