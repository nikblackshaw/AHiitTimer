package com.hiittimer.domain

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A complete description of an interval workout: the timings plus how they repeat.
 *
 * Presets are immutable and shipped with the app; user workouts are created
 * in the editor and stored on disk. Mirrors the iOS [Workout] model.
 */
@Serializable
data class Workout(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val warmUpSeconds: Int = 0,
    val workSeconds: Int,
    val restSeconds: Int,
    val coolDownSeconds: Int = 0,
    val rounds: Int = 1,
    val sets: Int = 1,
    val isPreset: Boolean = false,
    /** Spoken/displayed name per work interval, indexed by (set-1)*rounds + (round-1). */
    val exerciseNames: List<String>? = null,
) {
    object Limits {
        val nameLength = 1..40
        val warmUp = 0..600
        val work = 5..600
        val rest = 0..600
        val coolDown = 0..600
        val rounds = 1..20
        val sets = 1..10
        const val step = 5
    }

    /** Returns a copy clamped into [Limits] with a usable name. */
    fun validated(): Workout {
        val trimmed = name.trim()
        val safeName = if (trimmed.isEmpty()) "Untitled Workout"
            else trimmed.take(Limits.nameLength.last)
        return copy(
            name = safeName,
            warmUpSeconds = warmUpSeconds.coerceIn(Limits.warmUp),
            workSeconds = workSeconds.coerceIn(Limits.work),
            restSeconds = restSeconds.coerceIn(Limits.rest),
            coolDownSeconds = coolDownSeconds.coerceIn(Limits.coolDown),
            rounds = rounds.coerceIn(Limits.rounds),
            sets = sets.coerceIn(Limits.sets),
            exerciseNames = sanitised(exerciseNames, rounds.coerceIn(Limits.rounds) * sets.coerceIn(Limits.sets)),
        )
    }

    /** Total number of work intervals. */
    val workIntervalCount: Int get() = rounds * sets
    /** Total seconds of work. */
    val workDuration: Long get() = (workIntervalCount * workSeconds).toLong()
    /** Total seconds of rest. Every work interval is followed by a rest. */
    val restDuration: Long get() = (workIntervalCount * restSeconds).toLong()
    /** Length of the whole session including warm-up and cool-down. */
    val totalDuration: Long get() = (warmUpSeconds + coolDownSeconds).toLong() + workDuration + restDuration

    /** Short summary used in lists, e.g. "20s work · 10s rest · 8 rounds". */
    val summary: String get() =
        "${workSeconds}s work · ${restSeconds}s rest · $rounds round${if (rounds == 1) "" else "s"}"

    /** Extended summary that also mentions sets when there is more than one. */
    val detailedSummary: String get() = if (sets > 1) "$summary · $sets sets" else summary

    /** Exercise name for a 1-based set/round pair, or null when unnamed. */
    fun exerciseName(set: Int, round: Int): String? {
        val names = exerciseNames ?: return null
        if (set < 1 || round < 1 || rounds < 1) return null
        val index = (set - 1) * rounds + (round - 1)
        if (index >= names.size) return null
        return names[index].trim().ifEmpty { null }
    }

    companion object {
        private fun sanitised(names: List<String>?, count: Int): List<String>? {
            if (names == null) return null
            val cleaned = names.take(maxOf(count, 0)).map { it.trim() }
            return if (cleaned.all { it.isEmpty() }) null else cleaned
        }

        /** Curated starting points. Never mutated at runtime. */
        val presets: List<Workout> = listOf(
            Workout(id = "preset.tabata", name = "Tabata", workSeconds = 20, restSeconds = 10, rounds = 8, isPreset = true),
            Workout(id = "preset.4020", name = "40 / 20", workSeconds = 40, restSeconds = 20, rounds = 8, isPreset = true),
            Workout(id = "preset.3030", name = "30 / 30", workSeconds = 30, restSeconds = 30, rounds = 10, isPreset = true),
            Workout(id = "preset.tenMinute", name = "10-Minute Workout", workSeconds = 45, restSeconds = 15, rounds = 10, isPreset = true),
            Workout(id = "preset.sprint", name = "Sprint Intervals", warmUpSeconds = 60, workSeconds = 30, restSeconds = 60, coolDownSeconds = 60, rounds = 6, isPreset = true),
            Workout(id = "preset.beginner", name = "Beginner 20 / 40", workSeconds = 20, restSeconds = 40, rounds = 10, isPreset = true),
        )

        const val defaultPresetID = "preset.tabata"

        fun preset(withID: String): Workout? = presets.firstOrNull { it.id == withID }
    }
}
