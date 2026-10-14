package com.hiittimer.domain

import org.junit.Assert.*
import org.junit.Test

class WorkoutStateTest {

    @Test fun `clock formats`() {
        assertEquals("00:20", TimeFormat.clock(20.0))
        assertEquals("01:01:01", TimeFormat.clock(3661.0))
        assertEquals("00:00", TimeFormat.clock(-5.0))
    }

    @Test fun `duration formats`() {
        assertEquals("4:25", TimeFormat.duration(265.0))
        assertEquals("1:01:01", TimeFormat.duration(3661.0))
    }

    @Test fun `progress clamps`() {
        val plan = WorkoutPlan(Workout(name = "T", workSeconds = 20, restSeconds = 10, rounds = 1))
        val s = plan.state(at = 10.0)
        assertEquals(0.5, s.phaseProgress, 1e-9)
        assertTrue(s.totalProgress > 0 && s.totalProgress < 1)
    }

    @Test fun `announcements`() {
        val plan = WorkoutPlan(
            Workout(name = "T", workSeconds = 20, restSeconds = 10, rounds = 2,
                exerciseNames = listOf("Deadlifts", ""))
        )
        val work = plan.state(at = 0.0)
        assertEquals("Next exercise: Deadlifts.", work.speechAnnouncement)
        assertTrue(work.accessibilityAnnouncement.contains("Deadlifts"))
        assertTrue(work.accessibilityAnnouncement.contains("Round 1 of 2"))

        val rest = plan.state(at = 25.0)
        assertEquals("Rest.", rest.speechAnnouncement)

        val done = plan.state(at = 9999.0)
        assertEquals("Workout complete.", done.speechAnnouncement)
        assertEquals("Workout complete.", done.accessibilityAnnouncement)
    }
}
