package com.hiittimer.domain

import org.junit.Assert.*
import org.junit.Test

class WorkoutTest {

    @Test fun `validated clamps out-of-range values and names`() {
        val w = Workout(
            name = "  ",
            warmUpSeconds = -5, workSeconds = 10_000, restSeconds = -1,
            coolDownSeconds = 9999, rounds = 0, sets = 99,
        ).validated()
        assertEquals("Untitled Workout", w.name)
        assertEquals(0, w.warmUpSeconds)
        assertEquals(600, w.workSeconds)
        assertEquals(0, w.restSeconds)
        assertEquals(600, w.coolDownSeconds)
        assertEquals(1, w.rounds)
        assertEquals(10, w.sets)
    }

    @Test fun `validated trims long names`() {
        val w = Workout(name = "x".repeat(100), workSeconds = 20, restSeconds = 10).validated()
        assertEquals(40, w.name.length)
    }

    @Test fun `durations match iOS semantics - rest after every work interval`() {
        // 8 x 20/10 = 240s total (rest included after the last work interval).
        val tabata = Workout.presets[0]
        assertEquals(8 * 20L, tabata.workDuration)
        assertEquals(8 * 10L, tabata.restDuration)
        assertEquals(240L, tabata.totalDuration)
    }

    @Test fun `exerciseName resolves set-round index`() {
        val w = Workout(
            name = "T", workSeconds = 20, restSeconds = 10, rounds = 2, sets = 2,
            exerciseNames = listOf("A", "B", "C", " "),
        )
        assertEquals("A", w.exerciseName(1, 1))
        assertEquals("B", w.exerciseName(1, 2))
        assertEquals("C", w.exerciseName(2, 1))
        assertNull(w.exerciseName(2, 2))
        assertNull(w.exerciseName(0, 1))
    }

    @Test fun `sanitised drops all-blank names`() {
        val w = Workout(name = "T", workSeconds = 20, restSeconds = 10,
            exerciseNames = listOf(" ", "")).validated()
        assertNull(w.exerciseNames)
    }

    @Test fun `presets ship six workouts with tabata default`() {
        assertEquals(6, Workout.presets.size)
        assertEquals("preset.tabata", Workout.defaultPresetID)
        assertNotNull(Workout.preset("preset.tabata"))
    }

    @Test fun `summary strings`() {
        val w = Workout(name = "T", workSeconds = 20, restSeconds = 10, rounds = 8)
        assertEquals("20s work · 10s rest · 8 rounds", w.summary)
        assertEquals("20s work · 10s rest · 8 rounds", w.detailedSummary)
        val s2 = w.copy(sets = 3)
        assertTrue(s2.detailedSummary.contains("3 sets"))
    }
}
