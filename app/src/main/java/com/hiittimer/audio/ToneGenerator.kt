package com.hiittimer.audio

import com.hiittimer.domain.WorkoutPhase

/** Every distinct sound the app can make. Mirrors iOS AudioCue. */
enum class AudioCue {
    TICK, WARM_UP_START, WORK_START, REST_START, COOL_DOWN_START,
    WORKOUT_COMPLETE, START, PAUSE, RESUME;

    companion object {
        fun startOf(phase: WorkoutPhase): AudioCue = when (phase) {
            WorkoutPhase.WARM_UP -> WARM_UP_START
            WorkoutPhase.WORK -> WORK_START
            WorkoutPhase.REST -> REST_START
            WorkoutPhase.COOL_DOWN -> COOL_DOWN_START
        }
    }
}

interface AudioCuePlayer {
    var volume: Float
    fun play(cue: AudioCue)
    fun activate()
    fun deactivate()
}

/**
 * Generates short PCM tones in memory. Everything is produced locally, so the
 * app makes sound with no bundled resources and no network access.
 * Sample rate 44100, mono 16-bit — matches iOS ToneGenerator note map.
 */
object ToneGenerator {
    const val SAMPLE_RATE = 44_100
    data class Note(val frequency: Float, val durationSecs: Double)

    fun tone(frequency: Float, durationSecs: Double, amplitude: Float = 0.45f): ShortArray =
        sequence(listOf(Note(frequency, durationSecs)), amplitude, gapSecs = 0.0)

    fun sequence(notes: List<Note>, amplitude: Float = 0.45f, gapSecs: Double = 0.07): ShortArray {
        if (notes.isEmpty()) return ShortArray(0)
        val gapFrames = maxOf((gapSecs * SAMPLE_RATE).toInt(), 0)
        val noteFrames = notes.map { maxOf((it.durationSecs * SAMPLE_RATE).toInt(), 1) }
        val total = noteFrames.sum() + gapFrames * (notes.size - 1)
        if (total <= 0) return ShortArray(0)
        val out = ShortArray(total)
        var w = 0
        notes.forEachIndexed { index, note ->
            val frames = noteFrames[index]
            val attack = maxOf((0.004 * SAMPLE_RATE).toInt(), 1)
            val release = maxOf((0.02 * SAMPLE_RATE).toInt(), 1)
            for (frame in 0 until frames) {
                var env = 1f
                if (frame < attack) env = frame.toFloat() / attack
                else if (frame > frames - release) env = (frames - frame).toFloat() / release
                val t = frame.toFloat() / SAMPLE_RATE
                val v = kotlin.math.sin(2 * Math.PI.toFloat() * note.frequency * t) * env * amplitude
                out[w + frame] = (v * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
            }
            w += frames
            if (index < notes.size - 1) {
                for (f in 0 until gapFrames) out[w + f] = 0
                w += gapFrames
            }
        }
        return out
    }

    fun samplesFor(cue: AudioCue): ShortArray {
        fun n(vararg pairs: Pair<Float, Double>) = pairs.map { Note(it.first, it.second) }
        return when (cue) {
            AudioCue.TICK -> sequence(n(880f to 0.07), amplitude = 0.3f, gapSecs = 0.0)
            AudioCue.WARM_UP_START -> sequence(n(660f to 0.09, 660f to 0.09), amplitude = 0.4f)
            AudioCue.WORK_START -> sequence(n(1046f to 0.10, 1046f to 0.10), amplitude = 0.5f, gapSecs = 0.06)
            AudioCue.REST_START -> sequence(n(523f to 0.32), amplitude = 0.5f, gapSecs = 0.0)
            AudioCue.COOL_DOWN_START -> sequence(n(784f to 0.13, 660f to 0.24), amplitude = 0.45f)
            AudioCue.WORKOUT_COMPLETE -> sequence(
                n(784f to 0.13, 988f to 0.13, 1175f to 0.22), amplitude = 0.5f, gapSecs = 0.08)
            AudioCue.START -> sequence(n(1046f to 0.12, 1319f to 0.18), amplitude = 0.5f, gapSecs = 0.07)
            AudioCue.PAUSE -> sequence(n(440f to 0.14), amplitude = 0.4f, gapSecs = 0.0)
            AudioCue.RESUME -> sequence(n(880f to 0.14), amplitude = 0.4f, gapSecs = 0.0)
        }
    }
}
