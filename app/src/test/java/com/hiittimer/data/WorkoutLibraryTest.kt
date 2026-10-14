package com.hiittimer.data

import com.hiittimer.domain.Workout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WorkoutLibraryTest {

    private lateinit var files: MutableMap<String, String>
    private lateinit var store: WorkoutDataStore
    private lateinit var library: WorkoutLibrary

    @Before fun setUp() {
        files = mutableMapOf()
        store = object : WorkoutDataStore {
            override fun load(): String? = files["w"]
            override fun save(data: String) { files["w"] = data }
            override fun delete() { files.remove("w") }
        }
        library = WorkoutLibrary(store)
    }

    @Test fun `starts with presets only`() {
        assertTrue(library.customWorkouts.value.isEmpty())
        assertEquals(Workout.presets.size, library.all.size)
    }

    @Test fun `save and reload round-trips`() {
        val saved = library.save(Workout(name = "Mine", workSeconds = 30, restSeconds = 15, rounds = 5))
        assertFalse(saved.isPreset)
        val reloaded = WorkoutLibrary(store)
        assertEquals(1, reloaded.customWorkouts.value.size)
        assertEquals("Mine", reloaded.customWorkouts.value[0].name)
    }

    @Test fun `saving a preset creates a copy`() {
        val copy = library.save(Workout.presets[0])
        assertNotEquals(Workout.presets[0].id, copy.id)
        assertFalse(copy.isPreset)
        assertEquals(1, library.customWorkouts.value.size)
    }

    @Test fun `duplicate gives fresh identity`() {
        val a = library.save(Workout(name = "A", workSeconds = 30, restSeconds = 10))
        val b = library.duplicate(a)
        assertNotEquals(a.id, b.id)
        assertEquals(2, library.customWorkouts.value.size)
    }

    @Test fun `delete removes custom workout`() {
        val a = library.save(Workout(name = "A", workSeconds = 30, restSeconds = 10))
        library.delete(a.id)
        assertTrue(library.customWorkouts.value.isEmpty())
        library.delete("missing") // no crash
    }

    @Test fun `corrupt payload decodes to empty`() {
        files["w"] = "not json{{"
        assertTrue(WorkoutLibrary(store).customWorkouts.value.isEmpty())
    }
}
