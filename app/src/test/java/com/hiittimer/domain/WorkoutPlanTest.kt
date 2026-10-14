package com.hiittimer.domain

import org.junit.Assert.*
import org.junit.Test

class WorkoutPlanTest {

    @Test fun `tabata builds 16 phases totalling 240s`() {
        val plan = WorkoutPlan(Workout.presets[0])
        assertEquals(16, plan.phases.size)
        assertEquals(240.0, plan.totalDuration, 1e-9)
        assertEquals(WorkoutPhase.WORK, plan.phases.first().phase)
        assertEquals(WorkoutPhase.REST, plan.phases.last().phase)
    }

    @Test fun `warmup and cooldown wrap the intervals`() {
        val plan = WorkoutPlan(Workout.presets.first { it.id == "preset.sprint" })
        assertEquals(WorkoutPhase.WARM_UP, plan.phases.first().phase)
        assertEquals(WorkoutPhase.COOL_DOWN, plan.phases.last().phase)
        // 60 + 6*(30+60) + 60 = 660
        assertEquals(660.0, plan.totalDuration, 1e-9)
    }

    @Test fun `zero rest is skipped`() {
        val plan = WorkoutPlan(Workout(name = "T", workSeconds = 20, restSeconds = 0, rounds = 3))
        assertEquals(3, plan.phases.size)
        assertTrue(plan.phases.all { it.phase == WorkoutPhase.WORK })
    }

    @Test fun `state at boundaries`() {
        val plan = WorkoutPlan(Workout(name = "T", workSeconds = 20, restSeconds = 10, rounds = 2))
        // phases: W20 R10 W20 R10, total 60
        var s = plan.state(at = 0.0)
        assertEquals(0, s.phaseIndex)
        assertEquals(WorkoutPhase.WORK, s.phase.phase)

        s = plan.state(at = 19.999)
        assertEquals(0, s.phaseIndex)

        s = plan.state(at = 20.0)
        assertEquals(1, s.phaseIndex)
        assertEquals(WorkoutPhase.REST, s.phase.phase)

        s = plan.state(at = 60.0)
        assertTrue(s.isComplete)

        s = plan.state(at = 999.0)
        assertTrue(s.isComplete)
        assertEquals(60.0, s.totalElapsed, 1e-9)
    }

    @Test fun `exercise names flow into planned phases`() {
        val plan = WorkoutPlan(
            Workout(name = "T", workSeconds = 20, restSeconds = 10, rounds = 2,
                exerciseNames = listOf("Burpees", "Squats"))
        )
        assertEquals("Burpees", plan.phases[0].name)
        assertEquals("Squats", plan.phases[2].name)
        assertEquals("Burpees", plan.phases[0].displayName)
    }

    @Test fun `secondsRemaining rounds up`() {
        val plan = WorkoutPlan(Workout(name = "T", workSeconds = 20, restSeconds = 10, rounds = 1))
        val s = plan.state(at = 19.2)
        assertEquals(1, s.secondsRemaining)
    }
}
