package com.hiittimer.audio

import org.junit.Assert.*
import org.junit.Test

class ToneGeneratorTest {

    @Test fun `every cue renders non-empty PCM`() {
        for (cue in AudioCue.values()) {
            val samples = ToneGenerator.samplesFor(cue)
            assertTrue("$cue must render samples", samples.isNotEmpty())
            assertTrue("$cue must not be silent", samples.any { it != 0.toShort() })
        }
    }

    @Test fun `empty sequence renders nothing`() {
        assertEquals(0, ToneGenerator.sequence(emptyList()).size)
    }

    @Test fun `sample count matches durations`() {
        // 0.07s @ 44100Hz ≈ 3087 frames for a single tick.
        val tick = ToneGenerator.samplesFor(AudioCue.TICK)
        assertTrue(tick.size in 2800..3400)
        // Complete jingle is three notes + gaps, so strictly longer than the tick.
        assertTrue(ToneGenerator.samplesFor(AudioCue.WORKOUT_COMPLETE).size > tick.size)
    }

    @Test fun `startOf maps each phase`() {
        assertEquals(AudioCue.WARM_UP_START, AudioCue.startOf(com.hiittimer.domain.WorkoutPhase.WARM_UP))
        assertEquals(AudioCue.WORK_START, AudioCue.startOf(com.hiittimer.domain.WorkoutPhase.WORK))
        assertEquals(AudioCue.REST_START, AudioCue.startOf(com.hiittimer.domain.WorkoutPhase.REST))
        assertEquals(AudioCue.COOL_DOWN_START, AudioCue.startOf(com.hiittimer.domain.WorkoutPhase.COOL_DOWN))
    }
}
