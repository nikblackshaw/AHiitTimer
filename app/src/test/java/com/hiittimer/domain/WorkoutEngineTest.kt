package com.hiittimer.domain

import android.content.SharedPreferences
import com.hiittimer.audio.AudioCue
import com.hiittimer.audio.AudioCuePlayer
import com.hiittimer.data.Preferences
import com.hiittimer.data.PreferencesStore
import com.hiittimer.haptics.HapticCue
import com.hiittimer.haptics.HapticFeedback
import com.hiittimer.speech.SpeechPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

private class FakeAudio : AudioCuePlayer {
    override var volume: Float = 0.8f
    val played = mutableListOf<AudioCue>()
    override fun play(cue: AudioCue) { played.add(cue) }
    override fun activate() {}
    override fun deactivate() {}
}

private class FakeHaptics : HapticFeedback {
    val played = mutableListOf<HapticCue>()
    override fun play(cue: HapticCue) { played.add(cue) }
}

private class FakeSpeech : SpeechPlayer {
    val spoken = mutableListOf<String>()
    override fun speak(text: String, voiceId: String?, languageCode: String?, rate: Double, volume: Double) {
        spoken.add(text)
    }
    override fun stop() {}
}

private fun stubPrefs(initial: Preferences): PreferencesStore {
    val map = mutableMapOf<String, String?>()
    val editor = object : SharedPreferences.Editor {
        override fun putString(k: String?, v: String?) = apply { map[k!!] = v }
        override fun putStringSet(k: String?, v: MutableSet<String>?) = this
        override fun putInt(k: String?, v: Int) = this
        override fun putLong(k: String?, v: Long) = this
        override fun putFloat(k: String?, v: Float) = this
        override fun putBoolean(k: String?, v: Boolean) = this
        override fun remove(k: String?) = apply { map.remove(k) }
        override fun clear() = apply { map.clear() }
        override fun commit() = true
        override fun apply() {}
    }
    val sp = object : SharedPreferences {
        override fun getAll() = map.toMap()
        override fun getString(k: String?, d: String?) = map[k] ?: d
        override fun getStringSet(k: String?, d: MutableSet<String>?) = d
        override fun getInt(k: String?, d: Int) = d
        override fun getLong(k: String?, d: Long) = d
        override fun getFloat(k: String?, d: Float) = d
        override fun getBoolean(k: String?, d: Boolean) = d
        override fun contains(k: String?) = map.containsKey(k)
        override fun edit() = editor
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }
    val store = PreferencesStore(sp)
    store.update { initial }
    return store
}

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutEngineTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun engine(
        workout: Workout = Workout(name = "T", workSeconds = 20, restSeconds = 10, rounds = 2),
        prefs: Preferences = Preferences(),
        audio: FakeAudio = FakeAudio(),
        haptics: FakeHaptics = FakeHaptics(),
        speech: FakeSpeech = FakeSpeech(),
        now: LongArray = longArrayOf(1_000L),
    ): WorkoutEngine {
        val scope = CoroutineScope(dispatcher)
        return WorkoutEngine(
            plan = WorkoutPlan(workout),
            audio = audio, haptics = haptics,
            preferences = stubPrefs(prefs),
            speech = speech,
            clockMillis = { now[0] },
            scope = scope,
        )
    }

    @Test fun `start moves to running and cues work start`() {
        val audio = FakeAudio()
        val e = engine(audio = audio)
        assertEquals(EngineStatus.IDLE, e.status.value)
        e.start()
        assertEquals(EngineStatus.RUNNING, e.status.value)
        assertTrue(audio.played.contains(AudioCue.WORK_START))
        e.invalidate()
    }

    @Test fun `pause and resume preserve elapsed wall-clock time`() {
        val now = longArrayOf(10_000L)
        val e = engine(now = now)
        e.start()
        now[0] = 15_000L // +5s
        e.pause()
        assertEquals(EngineStatus.PAUSED, e.status.value)
        assertEquals(5.0, e.elapsedSecs, 0.05)
        now[0] = 60_000L // time passes while paused — must not count
        assertEquals(5.0, e.elapsedSecs, 0.05)
        e.resume()
        now[0] = 62_000L // +2s running
        assertEquals(7.0, e.elapsedSecs, 0.05)
        e.invalidate()
    }

    @Test fun `reset returns to idle at zero`() {
        val now = longArrayOf(5_000L)
        val e = engine(now = now)
        e.start()
        now[0] = 9_000L
        e.reset()
        assertEquals(EngineStatus.IDLE, e.status.value)
        assertEquals(0.0, e.elapsedSecs, 1e-9)
        e.invalidate()
    }

    @Test fun `finish at end of timeline`() {
        val now = longArrayOf(0L)
        val audio = FakeAudio()
        val speech = FakeSpeech()
        // 1 round: 20 work + 10 rest = 30s total
        val e = engine(
            workout = Workout(name = "T", workSeconds = 20, restSeconds = 10, rounds = 1),
            audio = audio, speech = speech, now = now,
        )
        e.start()
        now[0] = 31_000L
        e.tick()
        assertEquals(EngineStatus.FINISHED, e.status.value)
        assertTrue(e.state.value.isComplete)
        assertTrue(audio.played.contains(AudioCue.WORKOUT_COMPLETE))
        assertTrue(speech.spoken.contains("Workout complete."))
        e.invalidate()
    }

    @Test fun `phase change announces exercise and sound can be muted`() {
        val now = longArrayOf(0L)
        val audio = FakeAudio()
        val speech = FakeSpeech()
        val e = engine(
            workout = Workout(name = "T", workSeconds = 20, restSeconds = 10, rounds = 2,
                exerciseNames = listOf("Burpees", "Squats")),
            prefs = Preferences(soundEnabled = false),
            audio = audio, speech = speech, now = now,
        )
        e.start()
        assertTrue(audio.played.isEmpty()) // muted
        assertEquals("Next exercise: Burpees.", speech.spoken.firstOrNull())
        now[0] = 21_000L
        e.tick() // rest
        assertEquals("Rest.", speech.spoken.lastOrNull())
        e.invalidate()
    }

    @Test fun `countdown tick fires at 3-2-1`() {
        val now = longArrayOf(1_000L)
        val audio = FakeAudio()
        val prefs = Preferences(soundEnabled = true, countdownTicksEnabled = true)
        val e = engine(
            workout = Workout(name = "T", workSeconds = 10, restSeconds = 0, rounds = 1),
            prefs = prefs, audio = audio, now = now,
        )
        e.start() // t=0 of 10s work
        audio.played.clear()
        now[0] = 1_000L + 7_500L // 2.5s remaining -> tick "3"
        e.tick()
        now[0] = 1_000L + 8_500L // 1.5s remaining -> tick "2"
        e.tick()
        assertTrue(audio.played.contains(AudioCue.TICK))
        e.invalidate()
    }
}
