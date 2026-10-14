package com.hiittimer.data

import com.hiittimer.domain.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Storage backend for user workouts. Injected so tests can use a temp dir. */
interface WorkoutDataStore {
    fun load(): String?
    fun save(data: String)
    fun delete()
}

/** JSON file in the app files dir. */
class FileWorkoutStore(private val dir: File) : WorkoutDataStore {
    private val file: File get() = File(dir, "workouts.json")
    override fun load(): String? = try {
        val f = file
        if (!f.exists()) null else f.readText().ifEmpty { null }
    } catch (_: Exception) { null }
    override fun save(data: String) {
        try { dir.mkdirs(); file.writeText(data) } catch (_: Exception) { }
    }
    override fun delete() { try { file.delete() } catch (_: Exception) { } }
}

/** Presets plus saved workouts. Presets are read-only; editing one creates a copy. */
class WorkoutLibrary(private val store: WorkoutDataStore) {
    constructor(filesDir: File) : this(FileWorkoutStore(File(filesDir, "HiitTimer")))

    private val json = Json { ignoreUnknownKeys = true }
    private val _custom = MutableStateFlow(decode(store.load()))
    val customWorkouts: StateFlow<List<Workout>> = _custom.asStateFlow()

    /** Presets first, then user workouts in the order they were saved. */
    val all: List<Workout> get() = Workout.presets + _custom.value

    fun workout(withID: String): Workout? =
        Workout.preset(withID) ?: _custom.value.firstOrNull { it.id == withID }

    /** Inserts or replaces a user workout. Saving a preset creates a separate copy. */
    fun save(workout: Workout): Workout {
        var stored = workout.validated()
        if (Workout.preset(stored.id) != null) {
            stored = stored.copy(id = UUID.randomUUID().toString(), isPreset = false)
        }
        val list = _custom.value.toMutableList()
        val idx = list.indexOfFirst { it.id == stored.id }
        if (idx >= 0) list[idx] = stored else list.add(stored)
        _custom.value = list
        persist()
        return stored
    }

    /** Turns any workout into an editable user copy with a fresh identity. */
    fun duplicate(workout: Workout, name: String? = null): Workout {
        var copy = workout.copy(id = UUID.randomUUID().toString(), isPreset = false)
        if (name != null) copy = copy.copy(name = name)
        return save(copy)
    }

    fun delete(id: String) {
        val before = _custom.value.size
        _custom.value = _custom.value.filterNot { it.id == id }
        if (_custom.value.size != before) persist()
    }

    private fun persist() {
        try { store.save(json.encodeToString(ListSerializer(Workout.serializer()), _custom.value)) }
        catch (_: Exception) { }
    }

    private fun decode(raw: String?): List<Workout> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try { json.decodeFromString(ListSerializer(Workout.serializer()), raw) }
        catch (_: Exception) { emptyList() }
    }
}
